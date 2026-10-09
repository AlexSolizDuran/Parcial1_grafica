package grafica.ciudad;

import grafica.ciudad.construcciones.Auto;

/**
 * TRÁFICO AUTÓNOMO: VEHÍCULOS QUE CONDUCEN SOLOS.
 * Maneja tres coches que recorren las calles sin que el usuario los dirija. Cada uno
 * lleva su propia posición, orientación y velocidad, y elige su ruta entre las
 * intersecciones de la retícula de calles.
 *
 * <p>Un coche no apunta a cualquier punto del mapa: circula por el centro de las celdas
 * de calle y solo salta de intersección en intersección, así que por construcción nunca
 * atraviesa una manzana. Aun así, antes de aceptar la posición nueva se consulta
 * puedeCircular(), el mismo método que usa el vehículo del jugador. Si un día cambiara
 * el mapa, el tráfico se detendría en lugar de atravesar un edificio.
 *
 * <p>Ante cada intersección el coche frena y gira noventa grados. La frenada sale sola
 * de la curva de frenada (sqrt de dos veces la deceleración por la distancia restante):
 * al llegar al nodo ya va casi parado, gira dando un rodeo lento y vuelve a acelerar.
 *
 * <p>Está en este paquete y no en construcciones porque necesita los miembros protegidos
 * MAPA, centro() y puedeCircular() de la jerarquía, que no son visibles desde fuera.
 */
public final class Trafico {

    /** Número de coches que circulan a la vez; el enunciado pide al menos tres. */
    public static final int COCHES = 3;

    // ==================== 1. LA RETÍCULA DE INTERSECCIONES ====================

    /**
     * Las cuatro direcciones del mapa. El índice es la dirección: 0 norte, 1 sur,
     * 2 oeste, 3 este. Norte es menos Z, que es el frente cuando el ángulo vale cero.
     */
    private static final int[] DIRECCION_FILA = {-1, 1, 0, 0};
    private static final int[] DIRECCION_COLUMNA = {0, 0, -1, 1};

    /** Órbita de las direcciones: la opuesta a la cero es la uno, y a la dos la tres. */
    private static final int[] DIRECCION_OPUESTA = {1, 0, 3, 2};

    // ==================== 2. CONDUCCIÓN DE LOS COCHES ====================

    /**
     * Radio de captura de una intersección. El coche se ancla a su centro cuando está
     * más cerca que esto, y el salto es de menos de una unidad, así que no se ve.
     */
    private static final float UMBRAL_ENTRADA = 0.6f;

    /**
     * Avance mínimo que el coche debe hacer en un cuadro para que se considere que se
     * acerca a su nodo. Es menor que cualquier avance real, así que solo se dispara
     * cuando el coche está parado o cuando le ha dado la vuelta al nodo.
     */
    private static final float UMBRAL_AVANCE = 0.001f;

    /** Velocidad a la que se mueve mientras gira: lo bastante lenta para que se vea el giro. */
    private static final float VELOCIDAD_GIRO = 2.2f;

    /** Radianes de giro por segundo: fija cuánto tarda el coche en alinearse. */
    private static final float GIRO_POR_SEGUNDO = 3.2f;

    /**
     * Deceleración de la curva de frenada. La velocidad objetivo antes de una
     * intersección es la raíz de dos por esta constante por la distancia que falta,
     * de modo que el coche llega al nodo justo a punto de parar.
     */
    private static final float FRENADA_CURVA = 6f;

    /** Tasa con la que la velocidad se acerca a la objetivo, acelerando. */
    private static final float TASA_ACELERACION = 3f;

    /** Tasa con la que la velocidad se acerca a la objetivo, frenando. */
    private static final float TASA_FRENADO = 9f;

    // ==================== 3. SENTIDO ÚNICO Y PASO ====================

    /**
     * Las avenidas son de un solo sentido, alternados por parejas: las de índice par van
     * en un sentido y las siguientes en el contrario. Es la solución al encuentro de
     * frente. Dos coches que vienen el uno hacia el otro por la misma calle no existen,
     * así que no hace falta que uno se aparte ni que se ceda el paso en un cruce.
     *
     * <p>Sin esto, los dos coches que se cruzan irían por el mismo eje de la calle y
     * no podrían esquivarse: o se solapan o se quedan parados el uno frente al otro.
     */
    private static boolean sentidoPermitido(int fila, int columna, int direccion) {
        if (direccion == 0 || direccion == 1) { // Norte y sur usan la columna de la avenida.
            return (columna / 2) % 2 == (direccion == 0 ? 0 : 1);
        }
        return (fila / 2) % 2 == (direccion == 3 ? 0 : 1); // Oeste y este usan la fila.
    }

    /**
     * Separación lateral por debajo de la cual dos coches que avanzan en el mismo sentido
     * se consideran en el mismo carril. Con el eje de la calle, dos coches que van en
     * paralelo por la misma avenida se solapan si uno alcanza al otro.
     */
    private static final float ANCHO_CARRIL = 2f;

    /** Distancia a la que dos coches se tocan: la suma de sus radios. */
    private static final float DISTANCIA_CONTACTO = 2 * Conduccion.RADIO_AUTO;

    /**
     * Holgura que un coche que cede el paso deja con el que tiene preferencia.
     *
     * <p>Sin ella el problema es de geometría, no de frenada: el coche con preferencia
     * cruza la intersección por el centro y el que espera, pegado a su rueda, está justo
     * en medio del hueco que el otro necesita. Dejar margen hace que el que espera se
     * quede fuera de la intersección y el otro la cruce libre. Es la distancia de
     * seguridad de cualquier tráfico real.
     */
    private static final float MARGEN_SEGUIMIENTO = 5f;

    /** Distancia que se devuelve cuando no hay ningún vehículo al que ceder el paso. */
    private static final float SIN_OBSTACULO = 1000f;

    // ==================== 4. LOS TRES COCHES ====================

    /**
     * Intersección de partida de cada coche, como fila y columna de la retícula, y
     * dirección con la que arranca. Las tres respetan el sentido único de su avenida:
     * las columnas 2 y 6 van al sur, y la fila 4 va al este.
     */
    private static final int[][] NODOS_INICIO = {{4, 2}, {6, 6}, {4, 4}};

    /** Dirección con la que arranca cada coche: sur, sur y este. */
    private static final int[] DIRECCION_INICIO = {1, 1, 3};

    /** Velocidad de crucero de cada coche: distintos ritmos para que nomarchen en fila. */
    private static final float[] VELOCIDADES = {9f, 11f, 8f};

    /** Carrocerías de los coches del tráfico, distintas entre sí y del rojo del jugador. */
    private static final float[][] COLORES_CARROCERIA = {
        {0.25f, 0.72f, 0.35f},
        {0.20f, 0.45f, 0.85f},
        {0.92f, 0.78f, 0.20f}
    };

    /** Cabina clara y común, que separa el cristal del cuerpo de cada coche. */
    private static final float[] COLOR_CABINA = {0.85f, 0.90f, 0.95f};

    /**
     * Semilla del generador de números seudoaleatorios. Es fija a propósito: la ciudad
     * se recorre siempre igual, y las pruebas obtienen siempre el mismo tráfico.
     */
    private static final long SEMILLA = 20260309L;

    private final Conduccion control; // Dueño de la escena: aporta el mapa y las colisiones.
    private final Coche[] coches = new Coche[COCHES];
    private int semilla;

    /**
     * Prepara los tres coches sobre la retícula de calles y los deja listos para rodar.
     */
    public Trafico(Conduccion control) {
        this.control = control;
        reiniciar();
    }

    /**
     * Un coche del tráfico autonomouso. Lleva su propia posición, orientación y
     * velocidad, y además el nodo al que se dirige y su dirección de marcha, que es
     * lo que le permite elegir ruta en la próxima esquina.
     */
    static final class Coche {

        int indice; // Número de coche: ordena las prioridades al ceder el paso.
        float x; // Posición en la ciudad, en metros.
        float z;
        float angulo; // Orientación en radianes; cero apunta hacia menos Z.
        float velocidad; // Unidades por segundo; siempre positiva porque solo avanza.
        float velocidadCrucero; // Ritmo propio de este coche en tramo recto.
        int direccion; // Dirección de marcha, un índice de DIRECCION_FILA.
        int nodoFila; // Intersección que tiene delante.
        int nodoColumna;
        int girosConsecutivos; // Para no encadenar giros y quedarse dando bandos.
        boolean girando; // Verdadero mientras alinea el rumbo con la nueva dirección.
    }

    // ==================== 5. ESTADO Y REINICIO ====================

    /** Devuelve el coche del índice indicado; lo consultan el dibujo y las pruebas. */
    Coche coche(int indice) {
        return coches[indice];
    }

    /**
     * Recoloca los coches en su intersección de partida. Lo llama R para que un
     * recorrido nuevo vuelva a empezar con el tráfico donde estaba al abrir el programa.
     */
    public void reiniciar() {
        semilla = (int) SEMILLA;
        for (int indice = 0; indice < COCHES; indice++) {
            Coche coche = new Coche();
            int fila = NODOS_INICIO[indice][0];
            int columna = NODOS_INICIO[indice][1];
            coche.indice = indice;
            coche.direccion = DIRECCION_INICIO[indice];
            coche.x = Ciudad.centro(columna);
            coche.z = Ciudad.centro(fila);
            coche.nodoFila = fila + DIRECCION_FILA[coche.direccion];
            coche.nodoColumna = columna + DIRECCION_COLUMNA[coche.direccion];
            coche.angulo = anguloDe(coche.direccion);
            coche.velocidad = 0;
            coche.velocidadCrucero = VELOCIDADES[indice];
            coche.girosConsecutivos = 0;
            coche.girando = false;
            coches[indice] = coche;
        }
    }

    // ==================== 6. MOVIMIENTO ====================

    /**
     * Adelanta los tres coches un cuadro. El reloj es el mismo deltaTime de la
     * conducción del jugador, así que el tráfico tampoco depende de la tasa de cuadros.
     */
    public void actualizar(float deltaTime) {
        for (int indice = 0; indice < COCHES; indice++) {
            actualizarCoche(coches[indice], deltaTime);
        }
    }

    /**
     * Integra un cuadro del coche. El orden importa: primero comprueba si ya está en
     * la intersección, después gira si toca y solo entonces avanza.
     */
    private void actualizarCoche(Coche coche, float deltaTime) {
        float distancia = distanciaAlNodo(coche);
        if (distancia < UMBRAL_ENTRADA && !coche.girando) {
            entrarEnNodo(coche); // El hueco del nodo se comprueba dentro del propio método.
            distancia = distanciaAlNodo(coche);
        }

        // Mientras gira, el coche no se traslada: solo orienta sobre el centro de la
        // intersección. Así no avanza con el rumbo a medio alinear ni se sale de la calle.
        if (coche.girando) {
            girar(coche, deltaTime);
            coche.velocidad = 0;
            return;
        }

        float velocidadObjetivo = velocidadObjetivo(coche, distancia);
        coche.velocidad = acercar(coche.velocidad, velocidadObjetivo, deltaTime);

        float frenteX = -(float) Math.sin(coche.angulo);
        float frenteZ = -(float) Math.cos(coche.angulo);
        float siguienteX = coche.x + frenteX * coche.velocidad * deltaTime;
        float siguienteZ = coche.z + frenteZ * coche.velocidad * deltaTime;
        if (!puedeOcupar(coche, siguienteX, siguienteZ)) {
            // No cabe: el coche se queda donde está y se para, sin rebote. Es lo mismo
            // que hace el auto del jugador contra una manzana, contra un edificio o
            // contra otro coche. Las dos comprobaciones miran a dónde va, no a dónde
            // está, porque si no frenaría un cuadro tarde y llegaría a solaparse.
            coche.velocidad = 0;
            return;
        }
        coche.x = siguienteX;
        coche.z = siguienteZ;

        // Un cuadro muy largo puede dejar al coche más allá de la ventana de captura,
        // con su nodo ya a la espalda. Si en vez de acercarse se aleja del nodo, es que
        // lo ha rebasado: se ancla a él en lugar de quedarse empujando una pared.
        if (distanciaAlNodo(coche) >= distancia - UMBRAL_AVANCE) {
            entrarEnNodo(coche);
        }
    }

    /**
     * Velocidad que el coche debería tener este cuadro. Es la de crucero, salvo que
     * esté frenando para la esquina que tiene delante, para el vehículo al que cede
     * el paso o ya dentro de la intersección alineando su rumbo.
     */
    private float velocidadObjetivo(Coche coche, float distancia) {
        if (coche.girando) {
            return VELOCIDAD_GIRO;
        }
        // Raíz de la curva de frenada: a mitad de avenida el coche va a crucero y
        // va perdiendo velocidad según le falta para llegar a la intersección.
        float limite = (float) Math.sqrt(2 * FRENADA_CURVA * distancia);
        // La misma curva, pero aplicada al vehículo al que cede el paso y wanting dejar
        // el margen de seguridad: si está más cerca, tiene que frenar hasta pararse.
        float distanciaObstaculo = distanciaAlObstaculo(coche);
        if (distanciaObstaculo < DISTANCIA_CONTACTO + MARGEN_SEGUIMIENTO) {
            float hueco = distanciaObstaculo - DISTANCIA_CONTACTO;
            if (hueco <= 0) {
                return 0;
            }
            limite = Math.min(limite, (float) Math.sqrt(2 * FRENADA_CURVA * hueco));
        }
        return Math.min(coche.velocidadCrucero, limite);
    }

    /**
     * Distancia al vehículo al que este coche tiene que ceder el paso: el jugador o el
     * coche de menor índice que tenga por delante. Si no hay ninguno, devuelve un valor
     * alto para que la comprobación de arriba no llegue a activarse.
     */
    private float distanciaAlObstaculo(Coche coche) {
        float masCerca = SIN_OBSTACULO;
        float diferenciaJugadorX = control.autoX - coche.x;
        float diferenciaJugadorZ = control.autoZ - coche.z;
        float distanciaJugador = (float) Math.sqrt(
                diferenciaJugadorX * diferenciaJugadorX + diferenciaJugadorZ * diferenciaJugadorZ);
        if (distanciaJugador < masCerca) {
            masCerca = distanciaJugador;
        }
        float frenteX = -(float) Math.sin(coche.angulo);
        float frenteZ = -(float) Math.cos(coche.angulo);
        for (Coche otro : coches) {
            if (otro == coche || otro.indice >= coche.indice) {
                continue; // Solo cede ante quien tiene preferencia.
            }
            float diferenciaX = otro.x - coche.x;
            float diferenciaZ = otro.z - coche.z;
            if (diferenciaX * frenteX + diferenciaZ * frenteZ <= 0) {
                continue; // Está detrás: no hay nada que cederle.
            }
            float distancia = (float) Math.sqrt(diferenciaX * diferenciaX + diferenciaZ * diferenciaZ);
            if (distancia < masCerca) {
                masCerca = distancia;
            }
        }
        return masCerca;
    }

    /**
     * Indica si el coche puede ocupar esa posición. Se exigen tres cosas a la vez: que no
     * sea una manzana ni el borde del mapa, que no solape a ningún vehículo y que, si tiene
     * que ceder el paso, lo haga desde fuera del hueco que necesita el otro.
     */
    private boolean puedeOcupar(Coche coche, float x, float z) {
        if (!control.puedeCircular(x, z)) {
            return false; // Manzana o borde del mapa.
        }
        // El jugador manda siempre: no se le solapa en ninguna circunstancia.
        float distanciaJugador = distanciaA(control.autoX, control.autoZ, x, z);
        if (distanciaJugador < DISTANCIA_CONTACTO) {
            return false;
        }
        for (Coche otro : coches) {
            if (otro == coche) {
                continue;
            }
            float distancia = distanciaA(otro.x, otro.z, x, z);
            if (distancia < DISTANCIA_CONTACTO) {
                return false; // Con cualquier coche, tenga o no preferencia.
            }
        }
        // Ceder el paso significa quedarse fuera del recorrido del otro, no solo sin
        // tocarlo: si el que espera se para pegado al centro de la intersección, el que
        // tiene preferencia no tiene por dónde pasar y acabaría atravesándolo.
        for (Coche otro : coches) {
            if (otro == coche || otro.indice >= coche.indice) {
                continue;
            }
            if (distanciaA(otro.x, otro.z, x, z) < DISTANCIA_CONTACTO + MARGEN_SEGUIMIENTO) {
                return false;
            }
        }
        return true;
    }

    /** Distancia entre dos puntos de la ciudad. */
    private static float distanciaA(float x1, float z1, float x2, float z2) {
        float diferenciaX = x1 - x2;
        float diferenciaZ = z1 - z2;
        return (float) Math.sqrt(diferenciaX * diferenciaX + diferenciaZ * diferenciaZ);
    }

    /** Indica si algún coche del tráfico ocupa esa posición, con el círculo de colisión. */
    public boolean hayCocheEn(float x, float z) {
        for (Coche coche : coches) {
            float diferenciaX = coche.x - x;
            float diferenciaZ = coche.z - z;
            if (diferenciaX * diferenciaX + diferenciaZ * diferenciaZ
                    < DISTANCIA_CONTACTO * DISTANCIA_CONTACTO) {
                return true;
            }
        }
        return false;
    }

    /** Acerca la velocidad a la objetivo en proporción al tiempo, no al número de cuadros. */
    private float acercar(float actual, float objetivo, float deltaTime) {
        float tasa = objetivo > actual ? TASA_ACELERACION : TASA_FRENADO;
        float paso = 1 - (float) Math.exp(-tasa * deltaTime);
        return actual + (objetivo - actual) * paso;
    }

    /** Distancia que le queda al coche hasta el centro de la intersección que busca. */
    private float distanciaAlNodo(Coche coche) {
        float diferenciaX = Ciudad.centro(coche.nodoColumna) - coche.x;
        float diferenciaZ = Ciudad.centro(coche.nodoFila) - coche.z;
        return (float) Math.sqrt(diferenciaX * diferenciaX + diferenciaZ * diferenciaZ);
    }

    /**
     * El coche ya está en la intersección: se ancla a su centro, elige por dónde seguir
     * y se queda girando hasta alinear su rumbo con la nueva calle.
     *
     * <p>Anclarse es un salto hasta el centro del nodo, así que el hueco se comprueba
     * aquí dentro y no en quien llama. Si el centro está ocupado, el coche no se mueve ni
     * cambia de dirección y sigue esperando en la calle. Hacerlo en un solo sitio evita
     * que una de las dos llamadas se quede sin comprobar y meta dos coches en el mismo
     * punto.
     */
    private void entrarEnNodo(Coche coche) {
        float centroX = Ciudad.centro(coche.nodoColumna);
        float centroZ = Ciudad.centro(coche.nodoFila);
        if (!puedeOcupar(coche, centroX, centroZ)) {
            return; // El nodo está ocupado: espera en la calle, sin entrar en él.
        }
        coche.x = centroX;
        coche.z = centroZ;
        boolean antesGiraba = coche.girosConsecutivos > 0;
        int nuevaDireccion = elegirDireccion(coche);
        boolean ahoraGira = nuevaDireccion != coche.direccion;

        coche.direccion = nuevaDireccion;
        coche.girosConsecutivos = ahoraGira ? coche.girosConsecutivos + 1 : 0;
        coche.nodoFila += DIRECCION_FILA[nuevaDireccion];
        coche.nodoColumna += DIRECCION_COLUMNA[nuevaDireccion];
        coche.girando = ahoraGira;
        if (ahoraGira && !antesGiraba) {
            coche.velocidad = Math.min(coche.velocidad, VELOCIDAD_GIRO); // Entra despacio a la esquina.
        }
    }

    /**
     * Elige la dirección siguiente entre las que llevan a una calle de verdad y respetan
     * el sentido único de esa avenida. Nunca da media vuelta, y si el coche ya giró en
     * la última esquina prefiere seguir recto para no recorrer la ciudad dando bandos.
     */
    private int elegirDireccion(Coche coche) {
        int opuesta = DIRECCION_OPUESTA[coche.direccion];
        boolean[] valida = candidatasValidas(coche);
        int candidatas = 0;
        for (int direccion = 0; direccion < 4; direccion++) {
            if (valida[direccion]) {
                candidatas++;
            }
        }
        if (candidatas == 0) {
            return coche.direccion; // Nodo sin salida: se queda donde está, no se sale del mapa.
        }
        if (coche.girosConsecutivos > 0 && valida[coche.direccion]) {
            return coche.direccion; // Acaba de girar: al menos un tramo recto antes de volver a girar.
        }
        // Toma una de las candidatas al azar. Se cuenta cuántas hay, se elige el número
        // de orden entre ellas y se devuelve esa, no su índice de dirección.
        int orden = siguienteEntero(candidatas);
        int visto = 0;
        for (int direccion = 0; direccion < 4; direccion++) {
            if (!valida[direccion]) {
                continue;
            }
            if (visto == orden) {
                return direccion;
            }
            visto++;
        }
        return coche.direccion; // No se alcanza: solo si el mapa cambiara entre bucles.
    }

    /**
     * Marca qué direcciones salen de este nodo hacia una calle de verdad. Descarta la de
     * retroceso, la que no es calle y la que va en contra del sentido de su avenida.
     */
    private boolean[] candidatasValidas(Coche coche) {
        boolean[] valida = new boolean[4];
        int opuesta = DIRECCION_OPUESTA[coche.direccion];
        for (int direccion = 0; direccion < 4; direccion++) {
            if (direccion == opuesta) {
                continue;
            }
            int fila = coche.nodoFila + DIRECCION_FILA[direccion];
            int columna = coche.nodoColumna + DIRECCION_COLUMNA[direccion];
            if (!esCalle(fila, columna)) {
                continue;
            }
            valida[direccion] = sentidoPermitido(coche.nodoFila, coche.nodoColumna, direccion);
        }
        return valida;
    }

    /**
     * Gira el rumbo hacia el de su nueva calle a velocidad constante, de modo que el
     * giro se ve como un giro y no como un salto de un cuadro al siguiente.
     */
    private void girar(Coche coche, float deltaTime) {
        float objetivo = anguloDe(coche.direccion);
        float diferencia = objetivo - coche.angulo;
        while (diferencia > Math.PI) {
            diferencia -= 2 * Math.PI;
        }
        while (diferencia < -Math.PI) {
            diferencia += 2 * Math.PI;
        }
        float paso = GIRO_POR_SEGUNDO * deltaTime;
        if (Math.abs(diferencia) <= paso) {
            coche.angulo = objetivo; // Ya alineado: sale de la esquina.
            coche.girando = false;
        } else {
            coche.angulo += Math.signum(diferencia) * paso;
        }
    }

    // ==================== 7. UTILIDADES DE LA RETÍCULA ====================

    /** Indica si esas coordenadas caen dentro del mapa y sobre una celda de calle. */
    private boolean esCalle(int fila, int columna) {
        if (fila < 0 || columna < 0 || fila >= Ciudad.LADO || columna >= Ciudad.LADO) {
            return false;
        }
        return Ciudad.MAPA[fila][columna] == 0;
    }

    /** Ángulo del frente del coche que avanza en esa dirección; cero es norte. */
    private static float anguloDe(int direccion) {
        return (float) Math.atan2(-DIRECCION_COLUMNA[direccion], -DIRECCION_FILA[direccion]);
    }

    /**
     * Entero pseudoaleatorio entre cero y exclusive. Usa la semilla guardada en la clase
     * y la avanza, para que dos llamadas den números distintos y la secuencia sea
     * siempre la misma en cada partida.
     */
    private int siguienteEntero(int exclusive) {
        semilla = semilla * 1103515245 + 12345; // Generador lineal congruencial.
        int positivo = semilla & 0x7fffffff;
        return positivo % exclusive;
    }

    // ==================== 8. DIBUJO ====================

    /**
     * Dibuja los tres coches con el color de cada uno. Se omiten en el minimapa: allí son
     * motas de un píxel que no aportan nada y multiplican el coste del segundo pase.
     */
    public void dibujar() {
        if (control.vistaMapa) {
            return;
        }
        for (int indice = 0; indice < COCHES; indice++) {
            Coche coche = coches[indice];
            new Auto(control, coche.x, coche.z, coche.angulo,
                    COLORES_CARROCERIA[indice], COLOR_CABINA).dibujar();
        }
    }
}
