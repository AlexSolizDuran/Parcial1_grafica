package grafica.ciudad.construcciones;

import grafica.ciudad.Motor;

/**
 * CONSTRUCCIÓN: SEMÁFORO.
 * Poste, carcasa y las tres bombillas que van encendidas según la fase del ciclo.
 * Es decorativo: no detiene al vehículo. Su apariencia depende del reloj de la
 * simulación, así que hay que pasarle el tiempo en cada cuadro.
 */
public final class Semaforo {

    /** Segundos que dura el ciclo rojo, verde y amarillo. */
    public static final int CICLO = 10;

    /** El rojo ocupa los primeros cuatro segundos del ciclo. */
    public static final int SEGUNDOS_ROJO = 4;

    /** El ámbar ocupa los últimos dos segundos del ciclo. */
    private static final int SEGUNDOS_AMBAR = 2;

    /** Número de bombillas de la carcasa, de arriba abajo: rojo, ámbar y verde. */
    private static final int BOMBILLAS = 3;

    /** Separación vertical entre bombillas y distancia de la carcasa hacia la calle. */
    private static final float PASO_BOMBILLA = 0.35f;
    private static final float ALTURA_PRIMERA = 3.45f;
    private static final float SALIENTE = 0.24f;

    /** Brillo de una bombilla apagada: se ve sucia en vez de desaparecer. */
    private static final float BRILLO_APAGADA = 0.15f;

    private Semaforo() {
    }

    /** Dibuja el semáforo completo con la fase que le corresponde al tiempo dado. */
    public static void dibujar(Motor motor, float x, float z, float tiempo) {
        motor.caja(x, 1.7f, z, 0.18f, 2.8f, 0.18f, 0.18f, 0.20f, 0.22f); // Poste sobre la acera.
        motor.caja(x, 3.1f, z, 0.55f, 1.2f, 0.45f, 0.08f, 0.10f, 0.12f); // Carcasa.

        int fase = (int) (tiempo % CICLO); // La fase depende solo del reloj de la simulación.
        for (int bombilla = 0; bombilla < BOMBILLAS; bombilla++) { // Rojo arriba, ámbar en medio, verde abajo.
            boolean encendida = bombilla == 0 ? fase < SEGUNDOS_ROJO
                    : bombilla == 1 ? fase >= CICLO - SEGUNDOS_AMBAR
                    : fase >= SEGUNDOS_ROJO && fase < CICLO - SEGUNDOS_AMBAR;
            float brillo = encendida ? 1 : BRILLO_APAGADA;
            float rojo = bombilla < 2 ? brillo : 0; // Rojo y ámbar llevan componente roja.
            float verde = bombilla > 0 ? brillo : 0; // Ámbar y verde llevan componente verde.
            float altura = ALTURA_PRIMERA - bombilla * PASO_BOMBILLA;

            Runnable foco = () -> motor.caja(x, altura, z - SALIENTE, 0.28f, 0.25f, 0.06f, rojo, verde, 0.02f);
            if (encendida) {
                motor.conEmision(foco);
            } else {
                foco.run();
            }
        }
    }
}
