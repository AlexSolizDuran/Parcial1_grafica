package grafica.ciudad.construcciones;

import grafica.ciudad.Motor;

/**
 * CONSTRUCCIÓN: AUTOMÓVIL.
 * Carrocería, cabina, cuatro ruedas, faros y luces traseras del vehículo.
 *
 * <p>Es la única construcción con estado propio, porque todas sus piezas se dibujan en
 * el marco local girado del coche: posición, rumbo y eje vertical se fijan una vez en
 * el constructor y pieza() los aplica a cada caja. Así la matriz de rotación existe en
 * un solo sitio y no hay que repetirla en cada llamada.
 *
 * <p>El color se recibe en el constructor. El vehículo del jugador usa el rojo de siempre
 * y el tráfico autónomo pasa el color de cada coche, de modo que se distinguen entre sí.
 *
 * <p>Sus dimensiones tienen que caber dentro del círculo de colisión que fija
 * Conduccion.RADIO_AUTO; si se agranda el coche hay que agrandar también ese radio.
 */
public final class Auto {

    /** Distancia a la izquierda y a la derecha del centro del vehículo. */
    private static final float APARTAMIENTO_LATERAL = 0.88f;

    /** Distancia al eje delantero y al trasero. */
    private static final float APARTAMIENTO_FRONTAL = 0.82f;

    /** Carrocería y cabina del vehículo del jugador, que no indica color en su constructor. */
    private static final float[] ROJO_CARROCERIA = {0.95f, 0.24f, 0.12f};
    private static final float[] AZUL_CABINA = {0.22f, 0.65f, 0.78f};

    private final Motor motor;
    private final float centroX;
    private final float centroZ;
    private final float angulo;
    private final float[] colorCarroceria;
    private final float[] colorCabina;

    /**
     * Prepara el vehículo en un punto del mapa con un rumbo en radianes y el color
     * rojo del jugador. El frente del coche apunta hacia menos Z cuando el ángulo vale cero.
     */
    public Auto(Motor motor, float centroX, float centroZ, float angulo) {
        this(motor, centroX, centroZ, angulo, ROJO_CARROCERIA, AZUL_CABINA);
    }

    /**
     * Prepara el vehículo con el color que le corresponde. Los seis tonos son parámetros
     * del coche, no constantes de la clase, para que el tráfico pueda darle uno a cada uno.
     */
    public Auto(Motor motor, float centroX, float centroZ, float angulo,
            float[] colorCarroceria, float[] colorCabina) {
        this.motor = motor;
        this.centroX = centroX;
        this.centroZ = centroZ;
        this.angulo = angulo;
        this.colorCarroceria = colorCarroceria;
        this.colorCabina = colorCabina;
    }

    /** Dibuja la carrocería, la cabina, las cuatro ruedas y las luces. */
    public void dibujar() {
        pieza(0, 0.65f, 0, 1.65f, 0.55f, 2.6f, colorCarroceria[0], colorCarroceria[1], colorCarroceria[2]);
        pieza(0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, colorCabina[0], colorCabina[1], colorCabina[2]);

        float[] lados = {-APARTAMIENTO_LATERAL, APARTAMIENTO_LATERAL};
        float[] ejes = {-APARTAMIENTO_FRONTAL, APARTAMIENTO_FRONTAL};
        for (float lado : lados) {
            for (float eje : ejes) {
                pieza(lado, 0.38f, eje, 0.24f, 0.58f, 0.6f, 0.055f, 0.065f, 0.08f); // Rueda.
            }
        }

        float[] faros = {-0.55f, 0.55f};
        for (float faro : faros) {
            pieza(faro, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, 1, 0.95f, 0.65f); // Faro delantero, en menos Z.
            pieza(faro, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.85f, 0.05f, 0.05f); // Luz trasera.
        }
    }

    /**
     * Dibuja el halo emisivo de los faros. No comprueba si están encendidos: de eso
     * se encarga quien llama, que es quien conoce el estado del vehículo.
     */
    public void dibujarHalos() {
        motor.conEmision(() -> {
            float[] faros = {-0.55f, 0.55f};
            for (float faro : faros) {
                // Se dibuja una décima por delante de la pieza del faro para no coincidir en profundidad.
                pieza(faro, 0.68f, -1.4f, 0.5f, 0.3f, 0.06f, 1, 0.96f, 0.7f);
            }
        });
    }

    /**
     * Convierte una pieza del espacio local del auto al espacio de la ciudad.
     * La fórmula tiene que coincidir con la matriz giro del shader de vértices:
     * si cambia un signo, el coche sale espejado y no hay ningún error que lo delate.
     */
    private void pieza(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo);
        float seno = (float) Math.sin(angulo);
        float mundoX = centroX + coseno * x + seno * z;
        float mundoZ = centroZ - seno * x + coseno * z;
        motor.cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, angulo);
    }
}
