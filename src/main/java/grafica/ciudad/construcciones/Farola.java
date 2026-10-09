package grafica.ciudad.construcciones;

import grafica.ciudad.Motor;

/**
 * CONSTRUCCIÓN: FAROLA.
 * Poste y bombilla de una acera. Las diez posiciones viven en POSICIONES porque cumplen
 * dos papeles a la vez: dibujar el poste y decirle al shader dónde está cada luz. De noche
 * la bombilla brilla por sí misma; de día es una caja clara normal.
 *
 * <p>Cada farola está en el bordillo de la acera de una manzana, a una unidad del límite,
 * mirando a la calle: una farola real no se planta en mitad del asfalto. Como la acera va
 * de cero a ALTURA_ACERA, el poste arranca a esa altura y no en el suelo del asfalto.
 */
public final class Farola {

    /** Altura de la bombilla sobre el suelo. */
    public static final float ALTURA = 4.5f;

    /**
     * Cara superior de la acera. El poste arranca aquí: si se dibujara desde cero, con la
     * farola sobre la acera, el tramo de poste quedaría enterrado dentro de ella.
     */
    public static final float ALTURA_ACERA = 0.3f;

    /**
     * Posiciones de las diez bombillas, en el bordillo de la acera de diez manzanas
     * distintas: las esquinas de las manzanas que rodean a los cruces de la ciudad
     * ampliada. Cada una queda a una unidad del bordillo, mirando a la calle.
     */
    public static final float[][] POSICIONES = {
        {-36, ALTURA, -36}, {-24, ALTURA, -36}, {-36, ALTURA, -24},
        {24, ALTURA, -36}, {36, ALTURA, -36}, {24, ALTURA, -24},
        {-36, ALTURA, 24}, {-24, ALTURA, 24},
        {24, ALTURA, 24}, {36, ALTURA, 24}
    };

    private Farola() {
    }

    /** Dibuja el poste y, de noche, la bombilla encendida. */
    public static void dibujar(Motor motor, float x, float z, boolean noche) {
        float altoPoste = ALTURA - ALTURA_ACERA; // El poste va de la acera hasta la bombilla.
        motor.caja(x, ALTURA_ACERA + altoPoste / 2, z, 0.18f, altoPoste, 0.18f, 0.20f, 0.24f, 0.28f);

        Runnable bombilla = () -> motor.caja(x, ALTURA, z, 0.7f, 0.35f, 0.7f, 1, 0.83f, 0.42f);
        if (noche) {
            motor.conEmision(bombilla);
        } else {
            bombilla.run();
        }
    }

    /** Recorre las diez posiciones y dibuja una farola en cada una. */
    public static void dibujarTodas(Motor motor, boolean noche) {
        for (float[] posicion : POSICIONES) {
            dibujar(motor, posicion[0], posicion[2], noche);
        }
    }
}
