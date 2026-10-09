package grafica.ciudad;

import grafica.ciudad.construcciones.Edificio;
import grafica.ciudad.construcciones.Parque;

/**
 * PARTE 2: LA CIUDAD AMPLIADA A 11x11.
 * Hereda de Motor el esqueleto de OpenGL y añade el contenido de la ciudad:
 * el mapa de celdas, el suelo, las aceras, los edificios, los parques y las marcas
 * viales, más una cámara aérea que ya abarca los 110x110 unidades del mapa nuevo.
 * El mapa original de 7x7 se conserva intacto en la esquina noroeste.
 */
public class Ciudad extends Motor {

    // ==================== 1. MEDIDAS Y MAPA DE LA CIUDAD ====================

    public static final float CELDA = 10; // Ancho y profundidad de cada celda: no cambia respecto al 7x7.
    protected static final int LADO = 11; // Celdas por lado, el mínimo que pide el enunciado.
    protected static final float LIMITE = CELDA * LADO / 2; // 55 unidades del centro a cada borde.

    protected static final int[][] MAPA = { // 0 = calle, 1 = edificio, 2 = parque. Filas = Z, columnas = X.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Avenida del borde norte.
        {0, 1, 0, 1, 0, 2, 0, 1, 0, 1, 0}, // Sector nuevo al este de la manzana original.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Segunda avenida horizontal.
        {0, 2, 0, 1, 0, 1, 0, 2, 0, 1, 0}, // Mixto: dos manzanas originales y dos nuevas.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Avenida central.
        {0, 1, 0, 2, 0, 1, 0, 1, 0, 2, 0}, // Conserva las tres manzanas del 7x7 original.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Avenida del borde sur.
        {0, 1, 0, 1, 0, 1, 0, 1, 0, 2, 0}, // Franja nueva: cuatro edificios y un parque.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Séptima avenida horizontal.
        {0, 2, 0, 1, 0, 1, 0, 1, 0, 1, 0}, // Sector nuevo del sureste, con dos parques.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0} // Avenida del borde sur.
    };

    // ==================== 2. CONVERSIÓN DE ÍNDICES A COORDENADAS ====================

    /** Convierte el índice de una fila o columna en el centro de su celda. */
    protected static float centro(int indice) {
        return -LIMITE + CELDA * (indice + 0.5f); // Parte del borde negativo y avanza media celda.
    }

    /** Cuenta las parcelas de un tipo del mapa; lo usan el indicador y las pruebas. */
    protected int contarManzanas(int tipo) {
        int total = 0;
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                if (MAPA[fila][columna] == tipo) {
                    total++;
                }
            }
        }
        return total;
    }

    /**
     * Cota que alcanza el edificio más alto de la ciudad.
     * El marcador del auto en el minimapa se coloca por encima de este valor,
     * de modo que agrandar un edificio no puede dejar el coche tapado.
     */
    protected float cotaEdificioMasAlto() {
        float maxima = 0;
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                if (MAPA[fila][columna] == 1) {
                    maxima = Math.max(maxima, Edificio.altura(fila, columna));
                }
            }
        }
        return maxima;
    }

    // ==================== 3. DIBUJO DE LA CIUDAD ====================

    /** Recorre el mapa y convierte cada celda en geometría. */
    @Override // Sustituye el cubo de prueba de Motor por la ciudad.
    protected void escena() {
        dibujarSuelo();
        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre el mapa de norte a sur.
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                float x = centro(columna);
                float z = centro(fila);
                int tipo = MAPA[fila][columna];
                if (tipo == 0) {
                    dibujarMarcasCalle(fila, columna, x, z);
                } else {
                    dibujarAcera(x, z);
                    if (tipo == 1) {
                        Edificio.dibujar(this, x, z, fila, columna, esNoche());
                    } else {
                        Parque.dibujar(this, x, z);
                    }
                    decoracionDeManzana(tipo, fila, columna, x, z);
                }
            }
        }
    }

    /**
     * Si la escena está en ambiente nocturno, para que las construcciones decidan si
     * brillan por sí mismas. Aquí no hay luces, así que siempre es de día; la etapa que
     * las añade devuelve el valor real.
     */
    protected boolean esNoche() {
        return false;
    }

    /**
     * Detalle de cada manzana, que se dibuja justo después de su construcción.
     * Aquí no hay nada: la etapa final lo llena con su decoración urbana.
     */
    protected void decoracionDeManzana(int tipo, int fila, int columna, float x, float z) {
    }

    /** Asfalto de todo el mapa, con su cara superior en la altura cero. */
    private void dibujarSuelo() {
        float lado = LIMITE * 2;
        caja(0, -0.25f, 0, lado, 0.5f, lado, 0.16f, 0.19f, 0.23f);
    }

    /** Acera elevada de una manzana, diez unidades de lado. */
    private void dibujarAcera(float x, float z) {
        caja(x, 0.15f, z, CELDA, 0.3f, CELDA, 0.60f, 0.64f, 0.66f);
    }

    /** Líneas discontinuas de los tramos de calle, dejando los cruces despejados. */
    private void dibujarMarcasCalle(int fila, int columna, float x, float z) {
        if (fila % 2 == 0 && columna % 2 == 1) { // Tramo horizontal entre dos cruces.
            for (float desplazamiento = -3; desplazamiento <= 3; desplazamiento += 3) {
                caja(x + desplazamiento, 0.025f, z, 1.6f, 0.03f, 0.13f, 1, 0.84f, 0.35f);
            }
        }
        if (columna % 2 == 0 && fila % 2 == 1) { // Tramo vertical entre dos cruces.
            for (float desplazamiento = -3; desplazamiento <= 3; desplazamiento += 3) {
                caja(x, 0.025f, z + desplazamiento, 0.13f, 0.03f, 1.6f, 1, 0.84f, 0.35f);
            }
        }
    }

    // ==================== 4. CÁMARA AÉREA DE LA CIUDAD AMPLIADA ====================

    /** Orbita a distancia suficiente para encuadrar las 110 unidades del mapa nuevo. */
    @Override // La ciudad necesita una órbita mayor que la del cubo de prueba.
    protected void configurarCamara() {
        float radio = LIMITE * 1.9f; // 104.5 unidades: la ciudad completa entra en la imagen.
        float altura = LIMITE * 1.55f;
        vector("uOjo", (float) Math.sin(ORBITA_AEREA) * radio, altura, (float) Math.cos(ORBITA_AEREA) * radio);
        vector("uObjetivo", 0, 0, 0); // El objetivo sigue siendo el centro de la ciudad.
        decimal("uAspecto", (float) ancho / alto);
    }

    /** Identifica esta etapa en la barra de la ventana. */
    @Override
    protected String rotulo() {
        return "PARTE 2 | CIUDAD 11x11";
    }

    /** Publica el tamaño del mapa y el de las manzanas, que es lo nuevo frente al 7x7. */
    @Override
    protected String indicadores() {
        return " | " + LADO + "x" + LADO + " celdas de " + (int) CELDA + " u"
                + " | manzanas " + contarManzanas(1) + " con edificio, " + contarManzanas(2) + " parques";
    }
}