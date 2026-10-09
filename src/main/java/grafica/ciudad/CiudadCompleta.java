package grafica.ciudad;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_M;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glScissor;
import static org.lwjgl.opengl.GL11.glViewport;

import grafica.ciudad.construcciones.Semaforo;

/**
 * PARTE 5: CIUDAD TERMINADA, ENTREGAS, MINIMAPA Y PANEL.
 * Hereda de Iluminacion la ciudad, la conducción y las luces, y añade lo que faltaba
 * del enunciado: decoración urbana, semáforos animados por tiempo, tres entregas,
 * minimapa con M y un panel de información dentro de la ventana.
 * El minimapa y el panel se dibujan en un segundo y tercer pase, cada uno con su
 * recorte y su proyección, y ambos devuelven el estado de la pantalla principal.
 */
public class CiudadCompleta extends Iluminacion {

    // ==================== 1. ENTREGAS Y ESTADO DE LA PARTIDA ====================

    protected static final float RADIO_LLEGADA = 3; // Distancia al destino para poder entregar.
    protected static final float VELOCIDAD_ENTREGA = 1; // Hay que llegar frenando, por debajo de esta velocidad.
    private static final float SEPARACION_MARCADOR = 1.5f; // Altura libre sobre el edificio más alto del mapa.
    protected static final float[][] DESTINOS = { // Paradas escritas con centro() para no depender de números.
        {centro(9), centro(8)}, // (40, 30): esquina noreste de la ampliación.
        {centro(1), centro(4)}, // (-40, -10): esquina noroeste del núcleo original.
        {centro(4), centro(10)} // (-10, 50): avenida del borde sur.
    };

    private int entregas = 0; // Entregas completadas; a la vez señala cuál es el destino activo.
    private boolean mostrarMapa = true;
    private boolean partidaTerminada = false;
    private final Texto texto = new Texto(this);
    protected final Trafico trafico = new Trafico(this);

    /** Entregas ya completadas; lo consultan las pruebas. */
    protected int entregas() {
        return entregas;
    }

    /** Indica si el recorrido ya terminó; lo consultan las pruebas. */
    protected boolean partidaTerminada() {
        return partidaTerminada;
    }

    // ==================== 2. CONTROLES Y REINICIO ====================

    /** Añade M a los controles heredados. */
    @Override
    protected void tecla(int key) {
        super.tecla(key); // Conserva ESC, C, R, N y F.
        if (key == GLFW_KEY_M) {
            mostrarMapa = !mostrarMapa;
        }
    }

    /** Reinicia el vehículo, el tráfico y las entregas: la partida entera vuelve a empezar. */
    @Override
    protected void reiniciar() {
        super.reiniciar(); // Posición, orientación y velocidad del auto.
        trafico.reiniciar(); // Los coches vuelven a su intersección de partida.
        entregas = 0;
        partidaTerminada = false;
    }

    // ==================== 3. REGLAS DE LAS ENTREGAS ====================

    /**
     * El tráfico autónomo es lo único que se mueve por la ciudad aparte del jugador, así
     * que es esta etapa la que lo conoce. Devolverlo aquí hace que el auto del usuario
     * frene contra un coche igual que frena contra un edificio, sin rebote.
     */
    @Override
    protected boolean chocaConOtroVehiculo(float x, float z) {
        return trafico.hayCocheEn(x, z);
    }

    /** Actualiza la conducción y el tráfico, y comprueba si se completó la entrega activa. */
    @Override
    protected void actualizar(float deltaTime) {
        super.actualizar(deltaTime);
        // El tráfico va después de super: así esquiva al auto ya con su posición del cuadro.
        trafico.actualizar(deltaTime);
        if (partidaTerminada) {
            return; // El recorrido ya terminó: no se busca más destinos.
        }
        float distanciaX = autoX - DESTINOS[entregas][0];
        float distanciaZ = autoZ - DESTINOS[entregas][1];
        float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ;
        boolean estaCerca = distanciaCuadrada < RADIO_LLEGADA * RADIO_LLEGADA;
        boolean estaFrenando = Math.abs(velocidad) < VELOCIDAD_ENTREGA;
        if (estaCerca && estaFrenando) { // Llegar despacio vale; parar encima del objetivo, no.
            entregas++;
            partidaTerminada = entregas >= DESTINOS.length;
        }
    }

    /** Texto que aparece en la barra de la ventana. */
    @Override
    protected String indicadores() {
        String progreso = partidaTerminada
                ? "ENTREGAS " + DESTINOS.length + "/" + DESTINOS.length + " COMPLETADAS"
                : "ENTREGAS " + entregas + "/" + DESTINOS.length + " | DESTINO "
                        + (int) DESTINOS[entregas][0] + "," + (int) DESTINOS[entregas][1];
        return super.indicadores() + " | M: mapa | trafico " + Trafico.COCHES
                + " | " + progreso + " | " + (int) tiempo + " s";
    }

    // ==================== 4. ESCENA FINAL ====================

    /**
     * Añade a la ciudad, el vehículo y las luces heredados el destino de la entrega
     * activa y el tráfico autónomo. Los tres coches del tráfico van después de super
     * para que se dibujen sobre el asfalto y no bajo las manzanas.
     */
    @Override
    protected void escena() {
        super.escena(); // Ciudad, parcelas, vehículo, farolas y faros.
        if (!partidaTerminada) {
            dibujarDestino();
        }
        trafico.dibujar();
    }

    /**
     * Rellena el detalle de cada manzana que dejó vacío la clase Ciudad. Los edificios,
     * los parques y las farolas ya se dibujan dentro de su propia construcción; aquí solo
     * va lo que no pertenece a ninguna: el paso peatonal y los semáforos.
     */
    @Override
    protected void decoracionDeManzana(int tipo, int fila, int columna, float x, float z) {
        if (vistaMapa) {
            return; // En el minimapa los detalles pequeños no se distinguen y multiplican el coste.
        }
        dibujarPasoPeatonal(x, z);
        if ((fila + columna) % 3 == 0) { // Un semáforo de cada tres manzanas, para no saturar la escena.
            Semaforo.dibujar(this, x + 4, z - 4, tiempo);
        }
    }

    /** Coloca la marca dorada de la entrega activa. */
    private void dibujarDestino() {
        float x = DESTINOS[entregas][0];
        float z = DESTINOS[entregas][1];
        cajaEmisiva(x, 0.06f, z, 5, 0.08f, 5, 1, 0.72f, 0.12f); // Suelo dorado: tiene que verse también de noche.
        if (!vistaMapa) {
            float alturaBaliza = 3.5f + (float) Math.sin(tiempo * 2) * 0.3f; // Oscila suavemente.
            cajaGiradaEmisiva(x, alturaBaliza, z, 0.8f, 0.8f, 0.8f, 1, 0.8f, 0.15f, tiempo); // Cubo giratorio.
        }
    }

    // ==================== 5. VIALIDAD PEATONAL ====================

    /** Franjas blancas del paso peatonal sobre la calle contigua al norte de la manzana. */
    private void dibujarPasoPeatonal(float x, float z) {
        for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento++) {
            caja(x + desplazamiento, 0.045f, z - 8, 0.45f, 0.04f, 2, 0.85f, 0.87f, 0.83f);
        }
    }

    // ==================== 6. SEGUNDO Y TERCER PASE DE DIBUJO ====================

    /** Dibuja la escena principal, después el minimapa y después el panel de información. */
    @Override
    protected void dibujarFrame() {
        super.dibujarFrame(); // Escena en perspectiva, con su propia comprobación de errores.
        if (mostrarMapa) {
            dibujarMinimapa();
        }
        dibujarPanel();
    }

    /** Vista superior de la ciudad en un recuadro de su esquina, con norte hacia arriba. */
    private void dibujarMinimapa() {
        int dimensionMenor = Math.min(ancho, alto); // El lado se recalcula al redimensionar la ventana.
        int lado = Math.min(280, dimensionMenor / 3);
        int margen = Math.min(18, dimensionMenor / 20);
        int izquierda = ancho - lado - margen;
        int arriba = alto - lado - margen; // OpenGL mide Y desde abajo, así que "arriba" es el máximo.

        glEnable(GL_SCISSOR_TEST); // El recorte evita borrar el resto de la vista principal.
        glScissor(izquierda - 3, arriba - 3, lado + 6, lado + 6);
        glClearColor(0.80f, 0.87f, 0.94f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Marco del recuadro.
        glScissor(izquierda, arriba, lado, lado);
        glClearColor(0.06f, 0.10f, 0.15f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Fondo del mapa.

        glViewport(izquierda, arriba, lado, lado); // Redirige la proyección al recuadro.
        entero("uMapa", 1); // Vista superior ortográfica en el shader.
        decimal("uSemimapa", LIMITE + 3); // Tres unidades de aire alrededor del mapa.
        vistaMapa = true;
        try {
            escena(); // La misma ciudad, ahora vista desde arriba.
            dibujarIndicadorAuto();
        } finally {
            vistaMapa = false;
            entero("uMapa", 0);
            glDisable(GL_SCISSOR_TEST);
            glViewport(0, 0, ancho, alto); // El siguiente cuadro vuelve a la ventana completa.
        }
        if (glGetError() != GL_NO_ERROR) {
            throw new IllegalStateException("Error OpenGL en el minimapa");
        }
    }

    /** Marca cian con la punta blanca por encima de los edificios del mapa. */
    private void dibujarIndicadorAuto() {
        // Se coloca sobre la cota del edificio más alto para que nunca quede tapado.
        float altura = cotaEdificioMasAlto() + SEPARACION_MARCADOR;
        cajaGirada(autoX, altura, autoZ, 2.2f, 0.1f, 3.2f, 0.1f, 1, 1, angulo);
        float frenteX = -(float) Math.sin(angulo);
        float frenteZ = -(float) Math.cos(angulo);
        caja(autoX + frenteX * 2, altura + SEPARACION_MARCADOR, autoZ + frenteZ * 2, 0.9f, 0.1f, 0.9f, 1, 1, 1);
    }

    /**
     * Panel de información en la esquina superior izquierda. El shader proyecta estas
     * coordenadas en píxeles, así que no hace falta cambiar el viewport: solo el recorte
     * y la profundidad, porque el fondo 3D ya está escrito en esa zona de la ventana.
     */
    private void dibujarPanel() {
        String[] lineas = lineasDelPanel();
        float escala = Math.min(ancho, alto) * 0.0042f; // Píxeles que ocupa cada punto de la fuente.
        float altoLinea = 7 * escala; // Cinco puntos del glifo más uno de separación.
        float margen = 5 * escala;
        float anchoTexto = 0;
        for (String linea : lineas) {
            anchoTexto = Math.max(anchoTexto, Texto.ancho(linea, escala));
        }
        float arriba = alto - margen; // Borde superior del panel.
        float abajo = arriba - lineas.length * altoLinea - margen;
        float izquierda = margen;

        glEnable(GL_SCISSOR_TEST);
        glScissor((int) izquierda, (int) abajo, (int) (anchoTexto + margen * 2),
                (int) (arriba - abajo));
        glClearColor(0.05f, 0.07f, 0.10f, 0.92f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Solo el rectángulo del panel.

        entero("uMapa", 2); // El shader proyecta en píxeles, con la Y hacia arriba.
        glDisable(GL_DEPTH_TEST); // Sin esto el fondo 3D, que ya tiene profundidad, taparía el panel.
        try {
            float cursor = arriba - margen - 5 * escala; // La primera línea arranca arriba del todo.
            for (String linea : lineas) {
                texto.escribir(linea, izquierda + margen, cursor, escala, 0.92f, 0.95f, 1);
                cursor -= altoLinea;
            }
            dibujarBarraVelocidad(izquierda + margen, cursor - 1.5f * escala, anchoTexto, escala);
        } finally {
            glEnable(GL_DEPTH_TEST);
            entero("uMapa", 0);
            glDisable(GL_SCISSOR_TEST);
        }
    }

    /** Barra proporcional a la velocidad, justo debajo de los números. */
    private void dibujarBarraVelocidad(float x, float y, float anchoTotal, float escala) {
        float fraccion = Math.min(1, Math.abs(velocidad) / VELOCIDAD_MAXIMA);
        float alto = 2 * escala;
        caja(x + anchoTotal / 2, y, 0, anchoTotal, alto, escala, 0.20f, 0.24f, 0.30f); // Carril vacío.
        float ancho = anchoTotal * fraccion;
        if (ancho > escala) {
            caja(x + ancho / 2, y, escala, ancho, alto * 0.7f, escala, 0.15f, 0.85f, 0.95f); // Relleno.
        }
    }

    /** Texto que se muestra en el panel, actualizado en cada cuadro. */
    protected String[] lineasDelPanel() {
        String ambiente = noche ? "NOCHE" : "DIA";
        String farosTexto = faros ? "FAROS ON" : "FAROS OFF";
        String entregasTexto = partidaTerminada
                ? "ENTREGAS " + DESTINOS.length + "/" + DESTINOS.length + " LISTAS"
                : "ENTREGAS " + entregas + "/" + DESTINOS.length + "  RESTAN "
                        + (DESTINOS.length - entregas);
        String destinoTexto;
        if (partidaTerminada) {
            destinoTexto = "RECORRIDO COMPLETADO  R PARA JUGAR OTRA VEZ";
        } else {
            float distanciaX = autoX - DESTINOS[entregas][0];
            float distanciaZ = autoZ - DESTINOS[entregas][1];
            float distancia = (float) Math.sqrt(distanciaX * distanciaX + distanciaZ * distanciaZ);
            destinoTexto = "DESTINO " + (int) DESTINOS[entregas][0] + " " + (int) DESTINOS[entregas][1]
                    + "  DISTANCIA " + Math.round(distancia);
        }
        String nuevos = partidaTerminada ? "0" : String.valueOf(Math.round(Math.abs(velocidad) * 3.6f));
        return new String[]{
            "CIUDAD INTERACTIVA 11X11  " + (int) tiempo + " S",
            "VELOCIDAD " + nuevos + " KM/H   TRAFICO " + Trafico.COCHES + " COCHES",
            ambiente + "   " + farosTexto,
            entregasTexto,
            destinoTexto,
            "WASD FLECHAS CONDUCE   ESPACIO FRENA",
            "C CAMARA   R REINICIO   N DIA NOCHE   F FAROS   M MAPA"
        };
    }

    // ==================== 7. PUNTO DE ENTRADA ====================

    @Override
    protected String rotulo() {
        return "CIUDAD COMPLETA";
    }

    /** Punto de entrada de la versión final del proyecto. */
    public static void main(String[] args) {
        CiudadCompleta aplicacion = new CiudadCompleta();
        aplicacion.run();
    }
}