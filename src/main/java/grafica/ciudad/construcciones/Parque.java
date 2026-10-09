package grafica.ciudad.construcciones;

import grafica.ciudad.Ciudad;
import grafica.ciudad.Motor;

/**
 * CONSTRUCCIÓN: PARQUE.
 * Césped, árboles y banco de una manzana de parque. No necesita fila ni columna:
 * todos sus elementos están a la misma distancia del centro, así que la forma es
 * idéntica en las siete manzanas verdes de la ciudad.
 */
public final class Parque {

    /** Separación de cada árbol respecto al centro del parque, en las dos direcciones. */
    private static final float MARGEN_ARBOL = 2.5f;

    private Parque() {
    }

    /** Dibuja el parque entero: césped, cuatro árboles y un banco. */
    public static void dibujar(Motor motor, float x, float z) {
        motor.caja(x, 0.32f, z, Ciudad.CELDA - 1, 0.1f, Ciudad.CELDA - 1, 0.20f, 0.45f, 0.28f); // Césped.

        float[] desplazamientos = {-MARGEN_ARBOL, MARGEN_ARBOL}; // Cuatro posiciones en las esquinas.
        for (float desplazamientoX : desplazamientos) {
            for (float desplazamientoZ : desplazamientos) {
                float arbolX = x + desplazamientoX;
                float arbolZ = z + desplazamientoZ;
                motor.caja(arbolX, 1.2f, arbolZ, 0.35f, 2, 0.35f, 0.38f, 0.22f, 0.12f); // Tronco.
                motor.caja(arbolX, 2.7f, arbolZ, 2, 2.3f, 2, 0.12f, 0.42f, 0.23f); // Copa.
            }
        }

        motor.caja(x, 0.65f, z, 3, 0.25f, 0.8f, 0.55f, 0.30f, 0.13f); // Asiento.
        motor.caja(x, 0.4f, z, 2, 0.6f, 0.35f, 0.22f, 0.24f, 0.24f); // Soporte.
        motor.caja(x, 1, z + 0.35f, 3, 0.7f, 0.15f, 0.55f, 0.30f, 0.13f); // Respaldo.
    }
}
