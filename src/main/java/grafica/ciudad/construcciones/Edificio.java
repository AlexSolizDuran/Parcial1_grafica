package grafica.ciudad.construcciones;

import grafica.ciudad.Ciudad;
import grafica.ciudad.Motor;

/**
 * CONSTRUCCIÓN: EDIFICIO.
 * Un cuerpo con el alto y el tono que le tocan por su posición en el mapa, más las
 * ventanas de las cuatro fachadas. Solo se dibuja lo que pide el enunciado: que los
 * edificios varíen en altura y color, y que tengan ventanas que aparenten encenderse
 * de noche. Todo se dibuja desde un único método: quien llama no tiene que saber
 * cuántas piezas tiene ni si de noche hay que encender las ventanas.
 */
public final class Edificio {

    // ==================== 1. MEDIDAS ====================

    /** Base del cuerpo sobre la acera: coincide con el canto superior de la acera. */
    private static final float BASE = 0.3f;

    /** Ancho del cuerpo: el 70% de una celda, para que la acera asome alrededor. */
    private static final float ANCHO = Ciudad.CELDA * 0.7f;

    // ==================== 2. VENTANAS ====================

    /** Media anchura del cuerpo más un margen: separa la ventana de la fachada sin llegar a la acera. */
    private static final float SEPARACION_FACHADA = 3.51f;

    /** Alto, ancho y grosor de cada ventana. */
    private static final float VENTANA_ALTO = 0.9f;
    private static final float VENTANA_ANCHO = 0.8f;
    private static final float VENTANA_GROSOR = 0.04f;

    /** Primera ventana, paso de plantas y separación de ventanas dentro de cada fachada. */
    private static final float VENTANA_PRIMERA = 2f;
    private static final float ALTURA_PLANTA = 3f;
    private static final float VENTANA_ESPACIO = 2.2f;

    /** Cada cuántas ventanas hay una encendida de noche, sobre cinco. */
    private static final int ENCENDIDAS_DE_CADA_CINCO = 3;

    /**
     * El vidrio es una fracción fija del tono del cuerpo. No puede ser un color
     * suelto: la ventana y la fachada reciben la misma luz, así que un tono fijo solo
     * sería más claro que los edificios más oscuros y más oscuro que los más claros.
     * Con esta proporción la retícula se lee igual en los dieciocho, y de noche el
     * vidrio apagado es el mismo edificio visto a través de un hueco sin luz.
     */
    private static final float FACTOR_VIDRIO = 0.45f;
    private static final float TONO_VENTANA_ENCENDIDA = 0.95f;

    private Edificio() {
    }

    /** Altura del cuerpo del edificio; varía de forma reproducible entre 6 y 22. */
    public static float altura(int fila, int columna) {
        float altura = 6 + (fila * 5 + columna * 3) % 17;
        if (fila > 5 && columna > 5) {
            altura += 3; // Los sectores del sureste llevan los edificios más altos.
        }
        return altura;
    }

    /**
     * Tono del cuerpo según su posición, para que la ciudad no se vea uniforme.
     * Rellena el array que recibe en lugar de devolverlo, para no reservar memoria
     * una vez por edificio y cuadro.
     */
    public static void color(int fila, int columna, float[] color) {
        color[0] = 0.30f + ((fila + columna) % 3) * 0.12f; // Rojo por capas de tres en tres.
        color[1] = 0.34f + (columna % 4) * 0.07f; // Verde según la columna.
        color[2] = 0.46f + (fila % 5) * 0.06f; // Azul según la fila.
    }

    // ==================== 3. DIBUJO ====================

    /**
     * Dibuja el cuerpo del edificio y sus ventanas. De noche las ventanas encendidas
     * brillan por sí mismas; de día todas son un rectángulo oscuro que la luz del
     * ambiente se encarga de iluminar.
     */
    public static void dibujar(Motor motor, float x, float z, int fila, int columna, boolean noche) {
        float altura = altura(fila, columna);
        float[] color = new float[3];
        color(fila, columna, color);
        motor.caja(x, BASE + altura / 2, z, ANCHO, altura, ANCHO, color[0], color[1], color[2]);

        Runnable ventanas = () -> dibujarVentanas(motor, x, z, altura, fila, columna, color, noche);
        if (noche) {
            motor.conEmision(ventanas);
        } else {
            ventanas.run();
        }
    }

    /**
     * Ventanas en las cuatro fachadas. De noche solo se enciende una parte de ellas,
     * según una fórmula que depende de la manzana, la fachada y la planta: el barrio
     * no queda plano. De día todas están apagadas.
     */
    private static void dibujarVentanas(Motor motor, float x, float z, float altura, int fila, int columna, float[] color, boolean noche) {
        float[] desplazamientos = {-VENTANA_ESPACIO, VENTANA_ESPACIO}; // Dos ventanas por fachada.
        int planta = 0;
        for (float y = VENTANA_PRIMERA; y < altura - 1; y += ALTURA_PLANTA) {
            for (int fachada = 0; fachada < 4; fachada++) {
                int semilla = planta * 7 + fachada * 3 + fila * 5 + columna;
                boolean encendida = noche && semilla % 5 < ENCENDIDAS_DE_CADA_CINCO; // De día siempre apagadas.
                float rojo = encendida ? TONO_VENTANA_ENCENDIDA : color[0] * FACTOR_VIDRIO;
                float verde = encendida ? 0.75f : color[1] * FACTOR_VIDRIO;
                float azul = encendida ? 0.38f : color[2] * FACTOR_VIDRIO;
                for (float desplazamiento : desplazamientos) {
                    if (fachada == 0) {
                        motor.caja(x + desplazamiento, y, z - SEPARACION_FACHADA, VENTANA_ANCHO, VENTANA_ALTO, VENTANA_GROSOR, rojo, verde, azul);
                    } else if (fachada == 1) {
                        motor.caja(x + desplazamiento, y, z + SEPARACION_FACHADA, VENTANA_ANCHO, VENTANA_ALTO, VENTANA_GROSOR, rojo, verde, azul);
                    } else if (fachada == 2) {
                        motor.caja(x - SEPARACION_FACHADA, y, z + desplazamiento, VENTANA_GROSOR, VENTANA_ALTO, VENTANA_ANCHO, rojo, verde, azul);
                    } else {
                        motor.caja(x + SEPARACION_FACHADA, y, z + desplazamiento, VENTANA_GROSOR, VENTANA_ALTO, VENTANA_ANCHO, rojo, verde, azul);
                    }
                }
            }
            planta++;
        }
    }
}
