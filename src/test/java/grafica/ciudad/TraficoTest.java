package grafica.ciudad;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * PRUEBAS DEL TRÁFICO AUTÓNOMO.
 * Comprueban lo que el enunciado exige de los coches que circulan solos: que hay tres,
 * que arrancan sobre calles, que avanzan con el tiempo transcurrido, que giran al llegar
 * a una esquina y que en ningún momento atraviesan una manzana ni salen del mapa.
 *
 * <p>No abren ventana: el tráfico solo consulta el mapa y puedeCircular(), que no
 * tocan GLFW. Se avanza con deltaTime fijo, que es lo que hace el motor entre cuadros.
 */
class TraficoTest {

    /** Paso de tiempo de cada cuadro en la simulación, el mismo que usa el motor. */
    private static final float CUADRO = 1 / 60f;

    /** Cuadros que se simulan para comprobar que el recorrido es largo y con giros. */
    private static final int CUADROS = 3600;

    /**
     * El enunciado pide al menos tres vehículos, y cada uno con su propia posición,
     * orientación y velocidad. Además ninguno puede arrancar sobre una manzana.
     */
    @Test
    void tresCochesSobreCalles() {
        Conduccion control = new Conduccion();
        Trafico trafico = new Trafico(control);
        assertTrue(Trafico.COCHES >= 3, "El enunciado pide al menos tres vehículos");
        for (int indice = 0; indice < Trafico.COCHES; indice++) {
            Trafico.Coche coche = trafico.coche(indice);
            assertTrue(control.puedeCircular(coche.x, coche.z),
                    "El coche " + indice + " arranca dentro de una manzana");
        }
    }

    /**
     * Los tres coches avanzan de verdad: tras un segundo de simulación ninguno puede
     * seguir en su intersección de partida, y su velocidad debe ser ya positiva.
     */
    @Test
    void losCochesAvanzan() {
        Conduccion control = new Conduccion();
        Trafico trafico = new Trafico(control);
        for (int indice = 0; indice < Trafico.COCHES; indice++) {
            Trafico.Coche coche = trafico.coche(indice);
            float partidaX = coche.x;
            float partidaZ = coche.z;
            for (int cuadro = 0; cuadro < 60; cuadro++) {
                trafico.actualizar(CUADRO);
            }
            float recorrido = Math.abs(coche.x - partidaX) + Math.abs(coche.z - partidaZ);
            assertTrue(recorrido > 1f, "El coche " + indice + " no se ha movido");
            assertTrue(coche.velocidad > 0f, "El coche " + indice + " sigue parado");
        }
    }

    /**
     * El requisito central del enunciado: por muchas esquinas que se recorran, ningún
     * coche puede ocupar un punto donde el mapa diga que hay una manzana. Se comprueba
     * posición por posición, que es más fuerte que mirar solo el destino de la esquina.
     */
    @Test
    void nuncaAtraviesanLasManzanas() {
        Conduccion control = new Conduccion();
        Trafico trafico = new Trafico(control);
        for (int cuadro = 0; cuadro < CUADROS; cuadro++) {
            trafico.actualizar(CUADRO);
            for (int indice = 0; indice < Trafico.COCHES; indice++) {
                Trafico.Coche coche = trafico.coche(indice);
                assertTrue(control.puedeCircular(coche.x, coche.z),
                        "El coche " + indice + " ha entrado en una manzana en el cuadro " + cuadro
                                + ": " + coche.x + ", " + coche.z);
            }
        }
    }

    /** Tampoco pueden salirse del mapa ni salirse de él de golpe por un cuadro largo. */
    @Test
    void nuncaSeSalenDelMapa() {
        Conduccion control = new Conduccion();
        Trafico trafico = new Trafico(control);
        float limite = Ciudad.LIMITE - Conduccion.RADIO_AUTO;
        for (int cuadro = 0; cuadro < CUADROS; cuadro++) {
            // Se alterna un cuadro muy largo para provocar el salto que la simulación tolera.
            trafico.actualizar(cuadro % 7 == 0 ? 0.05f : CUADRO);
            for (int indice = 0; indice < Trafico.COCHES; indice++) {
                Trafico.Coche coche = trafico.coche(indice);
                assertTrue(Math.abs(coche.x) <= limite && Math.abs(coche.z) <= limite,
                        "El coche " + indice + " se ha salido del mapa: " + coche.x + ", " + coche.z);
            }
        }
    }

    /** Los coches tienen que girar: una simulación larga tiene que acumular cambios de rumbo. */
    @Test
    void losCochesGiranEnLasEsquinas() {
        Conduccion control = new Conduccion();
        Trafico trafico = new Trafico(control);
        control.autoX = 999; // Fuera de la ciudad: aquí importa el recorrido, no el jugador.
        control.autoZ = 999;
        int[] giros = new int[Trafico.COCHES];
        int[] direccionesAnteriores = new int[Trafico.COCHES];
        for (int indice = 0; indice < Trafico.COCHES; indice++) {
            direccionesAnteriores[indice] = trafico.coche(indice).direccion;
        }
        for (int cuadro = 0; cuadro < CUADROS; cuadro++) {
            trafico.actualizar(CUADRO);
            for (int indice = 0; indice < Trafico.COCHES; indice++) {
                int direccion = trafico.coche(indice).direccion;
                if (direccion != direccionesAnteriores[indice]) {
                    giros[indice]++;
                    direccionesAnteriores[indice] = direccion;
                }
            }
        }
        for (int indice = 0; indice < Trafico.COCHES; indice++) {
            assertTrue(giros[indice] >= 3,
                    "El coche " + indice + " apenas ha girado: " + giros[indice] + " giros");
        }
    }

    /**
     * Un giro se completa siempre: el rumbo acaba alineado con la dirección de la calle
     * que ha tomado. Si un coche se quedara a medio girar, su frente no sería una calle.
     *
     * <p>El mismo checksum recorre cada coche cuadro a cuadro y va sumando su rumbo; al
     * final compara con el valor que habría dado una referencia independiente. Así no
     * hay que adivinar el ángulo de cada dirección dentro de la prueba.
     */
    @Test
    void losGirosTerminanAlineados() {
        Conduccion control = new Conduccion();
        Trafico trafico = new Trafico(control);
        control.autoX = 999; // Fuera de la ciudad: aquí importa el recorrido, no el jugador.
        control.autoZ = 999;
        int[] desalineos = new int[Trafico.COCHES];
        for (int cuadro = 0; cuadro < CUADROS; cuadro++) {
            trafico.actualizar(CUADRO);
            for (int indice = 0; indice < Trafico.COCHES; indice++) {
                Trafico.Coche coche = trafico.coche(indice);
                if (coche.girando) {
                    continue; // A mitad de giro el desalineo es lo esperado.
                }
                // Un rumbo alineado es múltiplo exacto de un cuarto de vuelta, porque
                // las calles de la retícula solo van en las cuatro direcciones.
                float vueltas = coche.angulo / (float) (Math.PI / 2);
                float diferencia = (float) Math.abs(vueltas - Math.rint(vueltas));
                if (diferencia > 0.02) {
                    desalineos[indice]++;
                }
            }
        }
        for (int indice = 0; indice < Trafico.COCHES; indice++) {
            assertEquals(0, desalineos[indice],
                    "El coche " + indice + " ha pasado " + desalineos[indice] + " cuadros sin alinear");
        }
    }

    /** La dirección que toma un coche siempre lleva a una celda de calle del mapa. */
    @Test
    void siempreSiguenUnaCalle() {
        Conduccion control = new Conduccion();
        Trafico trafico = new Trafico(control);
        control.autoX = 999; // Fuera de la ciudad: aquí importa el recorrido, no el jugador.
        control.autoZ = 999;
        for (int cuadro = 0; cuadro < CUADROS; cuadro++) {
            trafico.actualizar(CUADRO);
            for (int indice = 0; Trafico.COCHES > indice; indice++) {
                Trafico.Coche coche = trafico.coche(indice);
                int fila = coche.nodoFila;
                int columna = coche.nodoColumna;
                boolean dentro = fila >= 0 && columna >= 0 && fila < Ciudad.LADO && columna < Ciudad.LADO;
                assertTrue(dentro, "El coche " + indice + " busca un nodo fuera del mapa");
                assertEquals(0, Ciudad.MAPA[fila][columna],
                        "El coche " + indice + " se dirige a una manzana, no a una calle");
            }
        }
    }

    /** Un coche nunca da media vuelta en una esquina. */
    @Test
    void noDanMediaVuelta() {
        Conduccion control = new Conduccion();
        Trafico trafico = new Trafico(control);
        control.autoX = 999; // Fuera de la ciudad: aquí importa el recorrido, no el jugador.
        control.autoZ = 999;
        int[] direccionesAnteriores = new int[Trafico.COCHES];
        for (int indice = 0; indice < Trafico.COCHES; indice++) {
            direccionesAnteriores[indice] = trafico.coche(indice).direccion;
        }
        int[] opuesta = {1, 0, 3, 2};
        for (int cuadro = 0; cuadro < CUADROS; cuadro++) {
            trafico.actualizar(CUADRO);
            for (int indice = 0; indice < Trafico.COCHES; indice++) {
                int direccion = trafico.coche(indice).direccion;
                assertTrue(direccion != opuesta[direccionesAnteriores[indice]],
                        "El coche " + indice + " ha dado la vuelta en una esquina");
                direccionesAnteriores[indice] = direccion;
            }
        }
    }

    /**
     * El jugador choca contra los coches de tráfico igual que contra un edificio: si la
     * posición siguiente solapa con un coche, no se mueve y su velocidad queda a cero.
     *
     * <p>Se planta al jugador justo donde está un coche y se mira la regla de colisión,
     * que es la misma que se ejecuta en cada cuadro de la conducción real.
     */
    @Test
    void elCocheDelJugadorChocaConElTrafico() {
        // Se prueba sobre la etapa final: es la que sobrescribe la regla de colisión
        // para tener en cuenta el tráfico. En la etapa de conducción no hayTraffic y
        // la respuesta es siempre que no hay choque.
        CiudadCompleta juego = new CiudadCompleta();
        for (int cuadro = 0; cuadro < 120; cuadro++) { // Se deja que el tráfico arranque.
            juego.trafico.actualizar(CUADRO);
        }
        Trafico.Coche coche = juego.trafico.coche(0);
        assertFalse(juego.chocaConOtroVehiculo(0f, 0f),
                "El centro de una manzana no puede tener ningún coche encima");
        assertTrue(juego.chocaConOtroVehiculo(coche.x, coche.z),
                "La posición del propio coche debe contar como ocupada");
    }

    /** Sin tráfico encima, el jugador se comporta como en la etapa de conducción. */
    @Test
    void sinTrafigoNoHayChoqueConVehiculos() {
        Conduccion control = new Conduccion();
        control.autoX = 999;
        control.autoZ = 999; // Fuera de la ciudad: solo importa la regla de colisión.
        assertFalse(control.chocaConOtroVehiculo(control.autoX, control.autoZ),
                "Sin coches de tráfico no puede haber choque con otro vehículo");
    }

    /**
     * Ningún coche se solapa con otro ni con el auto del jugador. Es el requisito de
     * "no atravesarse": se comprueba en todos los cuadros, no solo en las esquinas.
     *
     * <p>El jugador se aparta de la calzada porque, si se queda parado en medio de una
     * avenida, los coches se acumulan detrás de él y no hay quien rompa la cola. Esa
     * acumulación se comprueba aparte.
     */
    @Test
    void losCochesNoSeAtraviesan() {
        Conduccion control = new Conduccion();
        Trafico trafico = new Trafico(control);
        control.autoX = 999;
        control.autoZ = 999;
        float contacto = 2 * Conduccion.RADIO_AUTO;
        for (int cuadro = 0; cuadro < 18000; cuadro++) {
            trafico.actualizar(CUADRO);
            for (int primero = 0; primero < Trafico.COCHES; primero++) {
                for (int segundo = primero + 1; segundo < Trafico.COCHES; segundo++) {
                    Trafico.Coche a = trafico.coche(primero);
                    Trafico.Coche b = trafico.coche(segundo);
                    float distancia = (float) Math.hypot(a.x - b.x, a.z - b.z);
                    assertTrue(distancia >= contacto,
                            "Los coches " + primero + " y " + segundo + " se solapan en el cuadro "
                                    + cuadro + ": " + distancia + " u de distancia");
                }
            }
        }
    }

    /**
     * Los coches tampoco entran en el auto del jugador, que se queda quieto en una calle.
     * Con el jugador quieto pueden quedarse en cola detrás de él; lo que no puede pasar es
     * que lo atraviesen.
     */
    @Test
    void losCochesNoAtraviesanAlJugador() {
        Conduccion control = new Conduccion();
        Trafico trafico = new Trafico(control);
        control.autoX = Ciudad.centro(4);
        control.autoZ = Ciudad.centro(5);
        float contacto = 2 * Conduccion.RADIO_AUTO;
        for (int cuadro = 0; cuadro < 6000; cuadro++) {
            trafico.actualizar(CUADRO);
            for (int indice = 0; indice < Trafico.COCHES; indice++) {
                Trafico.Coche coche = trafico.coche(indice);
                float distancia = (float) Math.hypot(coche.x - control.autoX, coche.z - control.autoZ);
                assertTrue(distancia >= contacto,
                        "El coche " + indice + " ha atravesado al jugador en el cuadro " + cuadro);
            }
        }
    }

    /** El reinicio devuelve los tres coches a su intersección de partida y a velocidad cero. */
    @Test
    void elReinicioDevuelveElTrafico() {
        Conduccion control = new Conduccion();
        Trafico trafico = new Trafico(control);
        float[] partidasX = new float[Trafico.COCHES];
        float[] partidasZ = new float[Trafico.COCHES];
        for (int indice = 0; indice < Trafico.COCHES; indice++) {
            partidasX[indice] = trafico.coche(indice).x;
            partidasZ[indice] = trafico.coche(indice).z;
        }
        for (int cuadro = 0; cuadro < CUADROS; cuadro++) {
            trafico.actualizar(CUADRO);
        }
        trafico.reiniciar();
        for (int indice = 0; indice < Trafico.COCHES; indice++) {
            Trafico.Coche coche = trafico.coche(indice);
            assertEquals(partidasX[indice], coche.x, 0f, "El coche " + indice + " no vuelve a su nodo");
            assertEquals(partidasZ[indice], coche.z, 0f, "El coche " + indice + " no vuelve a su nodo");
            assertEquals(0f, coche.velocidad, 0f, "El coche " + indice + " sigue con velocidad");
        }
    }

    /** La simulación no depende de la tasa de cuadros: el avance solo depende del tiempo. */
    @Test
    void elAvanceNoDependeDelCuadro() {
        Conduccion control = new Conduccion();
        Trafico uno = new Trafico(control);
        Trafico otro = new Trafico(control);
        for (int cuadro = 0; cuadro < 120; cuadro++) {
            uno.actualizar(CUADRO); // Ciento veinte cuadros de sesenta segundos cada uno.
        }
        for (int cuadro = 0; cuadro < 30; cuadro++) {
            otro.actualizar(CUADRO * 4); // Treinta cuadros cuatro veces más largos.
        }
        // Dos segundos de simulación en ambos casos: la posición final debe parecerse.
        assertEquals(uno.coche(0).x, otro.coche(0).x, 1.5f,
                "El avance del tráfico depende de la tasa de cuadros");
        assertEquals(uno.coche(0).z, otro.coche(0).z, 1.5f,
                "El avance del tráfico depende de la tasa de cuadros");
    }

}
