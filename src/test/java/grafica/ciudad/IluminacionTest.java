package grafica.ciudad;

import grafica.ciudad.construcciones.Farola;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;



/**
 * PRUEBAS DE LA PARTE 4: LAS FAROLAS.
 * El enunciado exige al menos nueve farolas repartidas por calles y sectores nuevos.
 * Se comprueba que todas están sobre la acera, dentro del mapa y bien repartidas.
 */
class IluminacionTest {

    private int columnaDe(float x) {
        return (int) ((x + Ciudad.LIMITE) / Ciudad.CELDA); // Índice de celda que contiene esa coordenada.
    }

    private int filaDe(float z) {
        return (int) ((z + Ciudad.LIMITE) / Ciudad.CELDA);
    }

    /** El enunciado pide al menos nueve farolas: hay diez. */
    @Test
    void cantidadDeFarolas() {
        assertTrue(Farola.POSICIONES.length >= 9, "Hacen falta al menos nueve farolas");
        assertEquals(10, Farola.POSICIONES.length, "El proyecto usa diez");
    }

    /**
     * Cada farola tiene que estar sobre la acera de una manzana, no plantada en el asfalto.
     * Se comprueba que la celda es una manzana y que queda a una unidad del bordillo, que
     * es lo que la separa de la calle que ilumina.
     */
    @Test
    void farolasSobreAcera() {
        float mediaCelda = Ciudad.CELDA / 2;
        for (float[] luz : Farola.POSICIONES) {
            float x = luz[0];
            float z = luz[2];
            int fila = filaDe(z);
            int columna = columnaDe(x);
            assertTrue(Ciudad.MAPA[fila][columna] != 0,
                    "Hay una farola en plena calle en " + x + "," + z);
            float distanciaAlCentro = Math.abs(x - Ciudad.centro(columna));
            float distanciaAlCentroZ = Math.abs(z - Ciudad.centro(fila));
            assertTrue(Math.max(distanciaAlCentro, distanciaAlCentroZ) <= mediaCelda,
                    "La farola de " + x + "," + z + " se sale de su acera");
            assertTrue(mediaCelda - Math.max(distanciaAlCentro, distanciaAlCentroZ) <= 1.5f,
                    "La farola de " + x + "," + z + " debería estar junto al bordillo");
        }
    }

    /** Todas las farolas están dentro del mapa y a la misma altura. */
    @Test
    void farolasDentroDelMapa() {
        for (float[] luz : Farola.POSICIONES) {
            assertTrue(Math.abs(luz[0]) < Ciudad.LIMITE, "Fuera del mapa en X");
            assertTrue(Math.abs(luz[2]) < Ciudad.LIMITE, "Fuera del mapa en Z");
            assertEquals(Farola.ALTURA, luz[1], 0f, "Misma altura para todas");
        }
    }

    /** La red tiene que cubrir los cuatro cuadrantes, no solo el centro. */
    @Test
    void farolasRepartidas() {
        int conXPositiva = 0;
        int conXNegativa = 0;
        int conZPositiva = 0;
        int conZNegativa = 0;
        for (float[] luz : Farola.POSICIONES) {
            if (luz[0] > 0) {
                conXPositiva++;
            }
            if (luz[0] < 0) {
                conXNegativa++;
            }
            if (luz[2] > 0) {
                conZPositiva++;
            }
            if (luz[2] < 0) {
                conZNegativa++;
            }
        }
        assertTrue(conXPositiva >= 4 && conXNegativa >= 4, "Faltan farolas a los lados en X");
        assertTrue(conZPositiva >= 4 && conZNegativa >= 4, "Faltan farolas a los lados en Z");
    }

    /** El alcance de cada farola tiene que cubrir el ancho de una calle, o no iluminaría el suelo. */
    @Test
    void alcanceSuficienteParaLaCalle() {
        assertTrue(Iluminacion.ALCANCE_FAROLA > Ciudad.CELDA / 2,
                "La luz debe alcanzar la calle que tiene alrededor");
        assertTrue(Iluminacion.CENTRO_FARO > Iluminacion.APERTURA_FARO,
                "El centro del foco tiene que ser más estrecho que su borde");
    }
}