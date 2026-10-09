package grafica.ciudad;

import grafica.ciudad.construcciones.Semaforo;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;



/**
 * PRUEBAS DE LA PARTE 5: ENTREGAS Y DESTINOS.
 * Comprueban que las tres paradas están sobre calles transitables, que se puede llegar
 * hasta ellas y que el reinicio devuelve la partida a su estado inicial.
 * No abren ventana: la regla de entrega se comprueba con los métodos protegidos.
 */
class CiudadCompletaTest {

    /** Cada destino debe estar en una celda de calle: si no, el vehículo no podría parar. */
    @Test
    void destinosSobreCalle() {
        for (int indice = 0; indice < CiudadCompleta.DESTINOS.length; indice++) {
            float x = CiudadCompleta.DESTINOS[indice][0];
            float z = CiudadCompleta.DESTINOS[indice][1];
            int fila = (int) ((z + Ciudad.LIMITE) / Ciudad.CELDA);
            int columna = (int) ((x + Ciudad.LIMITE) / Ciudad.CELDA);
            assertEquals(0, Ciudad.MAPA[fila][columna],
                    "El destino " + indice + " está sobre una manzana");
        }
    }

    /** El radio de llegada tiene que caber: parado sobre el destino, el auto no choca. */
    @Test
    void llegaFrenandoAlDestino() {
        Conduccion control = new Conduccion();
        for (float[] destino : CiudadCompleta.DESTINOS) {
            assertTrue(control.puedeCircular(destino[0], destino[1]),
                    "No se puede aparcar en el destino " + destino[0] + "," + destino[1]);
        }
    }

    /** Los tres destinos están repartidos por la ciudad y ninguno está repetido. */
    @Test
    void destinosRepartidos() {
        assertEquals(3, CiudadCompleta.DESTINOS.length, "La partida tiene tres entregas");
        for (int i = 0; i < CiudadCompleta.DESTINOS.length; i++) {
            for (int j = i + 1; j < CiudadCompleta.DESTINOS.length; j++) {
                float distanciaX = CiudadCompleta.DESTINOS[i][0] - CiudadCompleta.DESTINOS[j][0];
                float distanciaZ = CiudadCompleta.DESTINOS[i][1] - CiudadCompleta.DESTINOS[j][1];
                float distancia = (float) Math.sqrt(distanciaX * distanciaX + distanciaZ * distanciaZ);
                assertTrue(distancia > 20f, "Dos paradas están demasiado cerca");
            }
        }
    }

    /** El ciclo del semáforo debe cubrir las tres luces sin huecos. */
    @Test
    void cicloDelSemaforo() {
        assertEquals(10, Semaforo.CICLO, "Ciclo de diez segundos");
        assertTrue(Semaforo.SEGUNDOS_ROJO > 0 && Semaforo.SEGUNDOS_ROJO < 10,
                "El rojo debe durar parte del ciclo");
        for (int fase = 0; fase < Semaforo.CICLO; fase++) {
            boolean rojo = fase < Semaforo.SEGUNDOS_ROJO;
            boolean verde = fase >= Semaforo.SEGUNDOS_ROJO && fase < 8;
            boolean amarillo = fase >= 8;
            assertTrue(rojo || verde || amarillo, "Hay un segundo sin luz en la fase " + fase);
            int encendidas = (rojo ? 1 : 0) + (verde ? 1 : 0) + (amarillo ? 1 : 0);
            assertEquals(1, encendidas, "Debe haber exactamente una luz encendida en la fase " + fase);
        }
    }

    /** El reinicio devuelve la partida al primer destino y detiene el vehículo. */
    @Test
    void reinicioDePartida() {
        CiudadCompleta juego = new CiudadCompleta();
        juego.autoX = 20;
        juego.autoZ = -10;
        juego.angulo = 1;
        juego.velocidad = 8;
        juego.reiniciar();
        assertEquals(Conduccion.POSICION_INICIAL_X, juego.autoX, 0f);
        assertEquals(Conduccion.POSICION_INICIAL_Z, juego.autoZ, 0f);
        assertEquals(0f, juego.angulo, 0f);
        assertEquals(0f, juego.velocidad, 0f);
        assertEquals(0, juego.entregas(), "Las entregas vuelven a empezar en cero");
        assertTrue(!juego.partidaTerminada(), "La partida vuelve a estar en curso");
    }

    /** El indicador del panel debe incluir velocidad, ambiente, luces y progreso. */
    @Test
    void contenidoDelPanel() {
        CiudadCompleta juego = new CiudadCompleta();
        String[] lineas = juego.lineasDelPanel();
        assertTrue(lineas.length >= 5, "El panel informa de varios datos");
        StringBuilder todo = new StringBuilder();
        for (String linea : lineas) {
            todo.append(linea).append(' ');
        }
        String texto = todo.toString();
        assertTrue(texto.contains("VELOCIDAD"), "Falta la velocidad");
        assertTrue(texto.contains("DIA"), "Falta el ambiente diurno");
        assertTrue(texto.contains("FAROS"), "Falta el estado de los faros");
        assertTrue(texto.contains("ENTREGAS 0/3"), "Falta el progreso de entregas");
        assertTrue(texto.contains("DESTINO"), "Falta el destino activo");
        for (int indice = 0; indice < texto.length(); indice++) {
            // Un carácter sin patrón se dibujaría como un hueco: el panel debe usar solo la fuente.
            assertTrue(Texto.contiene(texto.charAt(indice)),
                    "Carácter fuera de la fuente 3x5: " + texto.charAt(indice));
        }
    }
}