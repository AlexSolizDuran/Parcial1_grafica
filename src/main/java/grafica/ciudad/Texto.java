package grafica.ciudad;

/**
 * Fuente de mapa de bits 3x5 dibujada con cajas.
 * No usa imágenes ni tipografías externas: cada carácter es una rejilla de quince
 * posiciones y cada posición encendida es una caja del cubo unitario.
 * El texto se coloca en el espacio de pantalla del panel, de cero a uno.
 */
final class Texto {

    /** Caracteres disponibles, en el mismo orden que los patrones. */
    private static final String CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 :/.-+";

/** Cinco filas de tres columnas por carácter; el punto es hueco y la almohadilla encendida. */
    private static final String[][] PATRONES = {
        {".#.", "#.#", "###", "#.#", "#.#"}, // A
        {"##.", "#.#", "##.", "#.#", "##."}, // B
        {".##", "#..", "#..", "#..", ".##"}, // C
        {"##.", "#.#", "#.#", "#.#", "##."}, // D
        {"###", "#..", "##.", "#..", "###"}, // E
        {"###", "#..", "##.", "#..", "#.."}, // F
        {".##", "#..", "#.#", "#.#", ".##"}, // G
        {"#.#", "#.#", "###", "#.#", "#.#"}, // H
        {"###", ".#.", ".#.", ".#.", "###"}, // I
        {"..#", "..#", "..#", "#.#", ".#."}, // J
        {"#.#", "##.", "###", "##.", "#.#"}, // K
        {"#..", "#..", "#..", "#..", "###"}, // L
        {"#.#", "###", "###", "#.#", "#.#"}, // M
        {"#.#", "###", "###", "###", "#.#"}, // N
        {".#.", "#.#", "#.#", "#.#", ".#."}, // O
        {"##.", "#.#", "##.", "#..", "#.."}, // P
        {".#.", "#.#", "#.#", "###", ".##"}, // Q
        {"##.", "#.#", "##.", "#.#", "#.#"}, // R
        {".##", "#..", ".#.", "..#", "##."}, // S
        {"###", ".#.", ".#.", ".#.", ".#."}, // T
        {"#.#", "#.#", "#.#", "#.#", ".#."}, // U
        {"#.#", "#.#", "#.#", "#.#", ".#."}, // V
        {"#.#", "#.#", "###", "###", "#.#"}, // W
        {"#.#", "#.#", ".#.", "#.#", "#.#"}, // X
        {"#.#", "#.#", ".#.", ".#.", ".#."}, // Y
        {"###", "..#", ".#.", "#..", "###"}, // Z
        {"###", "#.#", "#.#", "#.#", "###"}, // 0
        {".#.", "##.", ".#.", ".#.", "###"}, // 1
        {"###", "..#", "###", "#..", "###"}, // 2
        {"###", "..#", "###", "..#", "###"}, // 3
        {"#.#", "#.#", "###", "..#", "..#"}, // 4
        {"###", "#..", "###", "..#", "###"}, // 5
        {"###", "#..", "###", "#.#", "###"}, // 6
        {"###", "..#", ".#.", ".#.", ".#."}, // 7
        {"###", "#.#", "###", "#.#", "###"}, // 8
        {"###", "#.#", "###", "..#", "###"}, // 9
        {"...", "...", "...", "...", "..."}, // espacio
        {"...", ".#.", "...", ".#.", "..."}, // :
        {"..#", "..#", ".#.", "#..", "#.."}, // /
        {"...", "...", "###", "...", "..."}, // -
        {"###", ".##", "###", "##.", "###"} // +
    };

    private final Motor motor; // Necesario para reutilizar el cubo unitario de la aplicación.

    Texto(Motor motor) {
        this.motor = motor;
    }

    /** Ancho que ocupará un texto con el tamaño indicado. */
    static float ancho(String texto, float escala) {
        return texto.length() * 4 * escala; // Tres columnas por carácter más una de separación.
    }

    /** Indica si la fuente tiene un patrón para ese carácter; lo usan las pruebas. */
    static boolean contiene(char caracter) {
        return CARACTERES.indexOf(Character.toUpperCase(caracter)) >= 0;
    }

    /** Dibuja una línea de texto; el origen es la esquina inferior izquierda del primer carácter. */
    void escribir(String texto, float x, float y, float escala, float r, float g, float b) {
        float cursor = x; // Avanza de cuatro en cuatro columnas al terminar cada carácter.
        for (int indice = 0; indice < texto.length(); indice++) {
            char caracter = texto.charAt(indice);
            int patron = CARACTERES.indexOf(Character.toUpperCase(caracter));
            if (patron >= 0) {
                dibujarCaracter(patron, cursor, y, escala, r, g, b);
            }
            cursor += 4 * escala; // Los caracteres desconocidos dejan un hueco y siguen avanzando.
        }
    }

    /** Enciende las cajas de las posiciones marcadas del patrón del carácter. */
    private void dibujarCaracter(int patron, float x, float y, float escala, float r, float g, float b) {
        String[] filas = PATRONES[patron];
        for (int fila = 0; fila < filas.length; fila++) {
            for (int columna = 0; columna < 3; columna++) {
                if (filas[fila].charAt(columna) != '#') {
                    continue;
                }
                // La fila cero del patrón es la de arriba, de ahí el invertido al calcular la altura.
                float centroX = x + (columna + 0.5f) * escala;
                float centroY = y + (4.5f - fila) * escala;
                motor.cajaGirada(centroX, centroY, 0, escala, escala, escala, r, g, b, 0);
            }
        }
    }
}