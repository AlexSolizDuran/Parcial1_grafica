package grafica.ciudad;

import grafica.ciudad.construcciones.Edificio;
import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;



/**
 * PRUEBAS DE LA PARTE 2: EL MAPA DE LA CIUDAD.
 * Comprueban el tamaño del mapa, la distribución urbana y que todas las calles queden
 * conectadas entre sí. No abren una ventana: solo leen el mapa estático de Ciudad.
 */
class CiudadTest {

    /** El enunciado exige al menos 11x11 celdas conservando el tamaño original de diez unidades. */
    @Test
    void mapaDeOncePorOnce() {
        assertEquals(Ciudad.LADO, 11, "El enunciado pide un mínimo de 11 celdas por lado");
        assertEquals(11, Ciudad.MAPA.length, "Deben existir once filas");
        for (int fila = 0; fila < Ciudad.MAPA.length; fila++) {
            assertEquals(11, Ciudad.MAPA[fila].length, "La fila " + fila + " debe medir once celdas");
        }
        assertEquals(10f, Ciudad.CELDA, 0f, "El tamaño de celda no puede cambiar");
        assertEquals(55f, Ciudad.LIMITE, 0f, "Media ciudad hacia cada borde");
    }

    /** Debe haber al menos doce manzanas con edificio y cuatro parques. */
    @Test
    void distribucionUrbanaMinima() {
        Ciudad ciudad = new Ciudad();
        assertTrue(ciudad.contarManzanas(1) >= 12, "Se piden 12 manzanas con edificio");
        assertTrue(ciudad.contarManzanas(2) >= 4, "Se piden 4 parques");
    }

    /** El 7x7 original debe seguir siendo idéntico en la esquina noroeste. */
    @Test
    void nucleoOriginalConservado() {
        int[][] esperado = { // El mapa de 7x7 del proyecto de referencia, fila a fila.
            {0, 0, 0, 0, 0, 0, 0},
            {0, 1, 0, 1, 0, 2, 0},
            {0, 0, 0, 0, 0, 0, 0},
            {0, 2, 0, 1, 0, 1, 0},
            {0, 0, 0, 0, 0, 0, 0},
            {0, 1, 0, 2, 0, 1, 0},
            {0, 0, 0, 0, 0, 0, 0}
        };
        for (int fila = 0; fila < 7; fila++) {
            for (int columna = 0; columna < 7; columna++) {
                assertEquals(esperado[fila][columna], Ciudad.MAPA[fila][columna],
                        "Cambió la celda original " + fila + "," + columna);
            }
        }
    }

    /**
     * Todas las celdas de calle deben formar una única red: se inundan desde el inicio y
     * se comprueba que se alcanzan todas las demás. Así ninguna manzana nueva parte el mapa.
     */
    @Test
    void redVialConectada() {
        boolean[][] visitado = new boolean[Ciudad.LADO][Ciudad.LADO];
        Deque<int[]> pendientes = new ArrayDeque<>();
        // El inicio del recorrido ocupa la celda 8,2, una intersección de la zona nueva.
        int[] inicio = new int[]{8, 2};
        pendientes.add(inicio);
        while (!pendientes.isEmpty()) {
            int[] celda = pendientes.poll();
            int fila = celda[0];
            int columna = celda[1];
            if (visitado[fila][columna] || Ciudad.MAPA[fila][columna] != 0) {
                continue; // Ya visitada o no es una calle.
            }
            visitado[fila][columna] = true;
            if (fila > 0) {
                pendientes.add(new int[]{fila - 1, columna});
            }
            if (fila < Ciudad.LADO - 1) {
                pendientes.add(new int[]{fila + 1, columna});
            }
            if (columna > 0) {
                pendientes.add(new int[]{fila, columna - 1});
            }
            if (columna < Ciudad.LADO - 1) {
                pendientes.add(new int[]{fila, columna + 1});
            }
        }
        int alcanzadas = 0;
        for (int fila = 0; fila < Ciudad.LADO; fila++) {
            for (int columna = 0; columna < Ciudad.LADO; columna++) {
                if (Ciudad.MAPA[fila][columna] == 0) {
                    assertTrue(visitado[fila][columna], "Celda de calle aislada en " + fila + "," + columna);
                    alcanzadas++;
                }
            }
        }
        assertTrue(alcanzadas > 60, "Un 11x11 con seis avenidas debe dejar más de 60 celdas de calle");
    }

    /** Toda manzana debe estar rodeada de calle: es lo que garantiza el acceso rodado. */
    @Test
    void cadaManzanaRodeadaDeCalle() {
        for (int fila = 0; fila < Ciudad.LADO; fila++) {
            for (int columna = 0; columna < Ciudad.LADO; columna++) {
                if (Ciudad.MAPA[fila][columna] == 0) {
                    continue;
                }
                assertEquals(0, Ciudad.MAPA[fila - 1][columna], "Falta calle al norte de " + fila + "," + columna);
                assertEquals(0, Ciudad.MAPA[fila + 1][columna], "Falta calle al sur de " + fila + "," + columna);
                assertEquals(0, Ciudad.MAPA[fila][columna - 1], "Falta calle al oeste de " + fila + "," + columna);
                assertEquals(0, Ciudad.MAPA[fila][columna + 1], "Falta calle al este de " + fila + "," + columna);
            }
        }
    }

    /** La conversión de índices a coordenadas debe colocar las esquinas dentro del mapa. */
    @Test
    void centrosCoherentes() {
        assertEquals(-50f, Ciudad.centro(0), 0.001f, "Primera fila, en el borde norte");
        assertEquals(0f, Ciudad.centro(5), 0.001f, "Fila central");
        assertEquals(50f, Ciudad.centro(10), 0.001f, "Última fila, en el borde sur");
    }

    /** Los edificios deben variar en altura para que la silueta no sea plana. */
    @Test
    void alturasVariadas() {
        float minima = Float.MAX_VALUE;
        float maxima = Float.MIN_VALUE;
        for (int fila = 0; fila < Ciudad.LADO; fila += 2) {
            for (int columna = 0; columna < Ciudad.LADO; columna += 2) {
                float altura = Edificio.altura(fila + 1, columna + 1);
                minima = Math.min(minima, altura);
                maxima = Math.max(maxima, altura);
            }
        }
        assertTrue(maxima - minima >= 10f, "Las alturas deben variar al menos diez unidades");
    }
}