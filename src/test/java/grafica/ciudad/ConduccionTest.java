package grafica.ciudad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * PRUEBAS DE LA PARTE 3: CONDUCCIÓN Y COLISIONES.
 * Comprueban la colisión del círculo del vehículo contra las manzanas y los bordes del
 * mapa ampliado, y el reinicio. No abren ventana: ningún método probado consulta GLFW.
 */
class ConduccionTest {

    /** El punto de partida y varias intersecciones del mapa nuevo deben ser transitables. */
    @Test
    void callesTransitables() {
        Conduccion juego = new Conduccion();
        assertTrue(juego.puedeCircular(Conduccion.POSICION_INICIAL_X, Conduccion.POSICION_INICIAL_Z),
                "El inicio debe estar sobre una calle");
        assertFalse(juego.puedeCircular(0, 0), "El origen es una manzana con edificio");
        assertTrue(juego.puedeCircular(40, 30), "Intersección del sector nuevo al este");
        assertFalse(juego.puedeCircular(-40, 40), "Allí hay un parque del sureste");
        assertTrue(juego.puedeCircular(30, -50), "Avenida del borde norte");
    }

    /** Ninguna manzana del mapa, ni de edificio ni de parque, puede recorrerse. */
    @Test
    void manzanasBloqueadas() {
        Conduccion juego = new Conduccion();
        for (int fila = 0; fila < Ciudad.LADO; fila++) {
            for (int columna = 0; columna < Ciudad.LADO; columna++) {
                if (Ciudad.MAPA[fila][columna] == 0) {
                    continue;
                }
                float x = Ciudad.centro(columna);
                float z = Ciudad.centro(fila);
                assertFalse(juego.puedeCircular(x, z), "Manzana no bloqueada en " + fila + "," + columna);
            }
        }
    }

    /** El radio del vehículo debe respetarse: basta un pelo de separación para chocar. */
    @Test
    void radioDelVehiculo() {
        Conduccion juego = new Conduccion();
        float demasiadoCerca = Ciudad.CELDA / 2 + Conduccion.RADIO_AUTO - 0.15f;
        assertFalse(juego.puedeCircular(0, demasiadoCerca), "El círculo invade la acera");
        float suficiente = Ciudad.CELDA / 2 + Conduccion.RADIO_AUTO + 0.15f;
        assertTrue(juego.puedeCircular(0, suficiente), "Ya hay separación suficiente");
    }

    /** Los cuatro bordes del mapa ampliado rechazan las posiciones exteriores. */
    @Test
    void bordesDelMapa() {
        Conduccion juego = new Conduccion();
        float limite = Ciudad.LIMITE - Conduccion.RADIO_AUTO;
        assertTrue(juego.puedeCircular(limite, 0), "Justo en el borde todavía cabe");
        assertFalse(juego.puedeCircular(limite + 0.5f, 0), "Al este no cabe");
        assertFalse(juego.puedeCircular(0, limite + 0.5f), "Al sur no cabe");
        assertFalse(juego.puedeCircular(limite + 0.5f, limite + 0.5f), "En la esquina no cabe");
        assertFalse(juego.puedeCircular(Ciudad.LIMITE + 5, 0), "Fuera del mapa, en cualquier caso");
    }

    /** La reinicialización devuelve posición, orientación y velocidad a su estado inicial. */
    @Test
    void reinicio() {
        Conduccion juego = new Conduccion();
        juego.autoX = 12;
        juego.autoZ = 4;
        juego.angulo = 2;
        juego.velocidad = 10;
        juego.reiniciar();
        assertEquals(Conduccion.POSICION_INICIAL_X, juego.autoX, 0f);
        assertEquals(Conduccion.POSICION_INICIAL_Z, juego.autoZ, 0f);
        assertEquals(0f, juego.angulo, 0f);
        assertEquals(0f, juego.velocidad, 0f);
    }

    /** El frente del vehículo apunta a menos Z con ángulo cero y rota con el seno y el coseno. */
    @Test
    void frenteDelVehiculo() {
        Conduccion juego = new Conduccion();
        juego.angulo = 0;
        assertEquals(-1f, -Math.cos(juego.angulo), 0.001f, "Con ángulo cero el frente es menos Z");
        assertEquals(0f, -Math.sin(juego.angulo), 0.001f, "Y no tiene componente en X");
    }
}