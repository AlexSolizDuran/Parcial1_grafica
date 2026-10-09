package grafica.ciudad;

import grafica.ciudad.construcciones.Auto;
import static org.lwjgl.glfw.GLFW.*;


/**
 * PARTE 3: EL AUTOMÓVIL Y LAS COLISIONES.
 * Hereda de Ciudad el mapa y el dibujo de la ciudad, y añade el vehículo: aceleración,
 * resistencia, freno, giro dependiente de la velocidad, colisión contra las manzanas
 * y los bordes, cámara de seguimiento y reinicio con R.
 * El frente del auto es -Z cuando el ángulo vale cero; los giros son radianes.
 */
public class Conduccion extends Ciudad {

    // ==================== 1. ESTADO DEL AUTO ====================

    protected static final float POSICION_INICIAL_X = -30; // Intersección dentro de la ciudad ampliada.
    protected static final float POSICION_INICIAL_Z = 30;
    protected static final float RADIO_AUTO = 1.65f; // Conserva dentro del círculo toda la carrocería.
    protected static final float ACELERACION = 9; // Unidades por segundo cuadrado.
    protected static final float RESISTENCIA_ROLLADO = 0.7f; // Pérdida de velocidad por segundo al rodar.
    protected static final float RESISTENCIA_FRENO = 7; // Pérdida mucho mayor mientras se mantiene el freno.
    protected static final float VELOCIDAD_MAXIMA = 16;
    protected static final float VELOCIDAD_MAXIMA_REVERSA = -6;
    protected static final float GIRO_POR_VELOCIDAD = 0.11f; // Radianes de giro por unidad de velocidad.
    protected static final float DISTANCIA_CAMARA = 12; // Unidades por detrás del auto para la cámara de seguimiento.
    protected static final float ALTURA_CAMARA = 9;

    protected float autoX = POSICION_INICIAL_X;
    protected float autoZ = POSICION_INICIAL_Z;
    protected float angulo = 0; // Orientación en radianes; cero apunta hacia menos Z.
    protected float velocidad = 0; // Unidades por segundo; negativa significa marcha atrás.
    protected boolean camaraAerea = false; // False: seguir el auto. True: ver toda la ciudad.

    // ==================== 2. TECLADO Y REINICIO ====================

    /** Añade los controles de cámara y de reinicio a los de Ciudad y Motor. */
    @Override
    protected void tecla(int key) {
        super.tecla(key); // Conserva ESC y el resto de lo que añadan las etapas anteriores.
        if (key == GLFW_KEY_C) {
            camaraAerea = !camaraAerea;
        }
        if (key == GLFW_KEY_R) {
            reiniciar();
        }
    }

    /** Devuelve el vehículo a su punto de partida. La etapa final reinicia también las entregas. */
    protected void reiniciar() {
        autoX = POSICION_INICIAL_X;
        autoZ = POSICION_INICIAL_Z;
        angulo = 0;
        velocidad = 0;
    }

    // ==================== 3. MOVIMIENTO POR CUADRO ====================

    /** Integra aceleración, resistencia, giro y colisiones con el tiempo del cuadro. */
    @Override // Sustituye la órbita con flechas de Ciudad, que ahora dirigen el volante.
    protected void actualizar(float deltaTime) {
        float acelerador = 0;
        if (pulsada(GLFW_KEY_W) || pulsada(GLFW_KEY_UP)) {
            acelerador += 1;
        }
        if (pulsada(GLFW_KEY_S) || pulsada(GLFW_KEY_DOWN)) {
            acelerador -= 1; // Primero frena y después entra en marcha atrás.
        }
        velocidad += acelerador * ACELERACION * deltaTime;

        float resistencia = RESISTENCIA_ROLLADO;
        if (pulsada(GLFW_KEY_SPACE)) {
            resistencia = RESISTENCIA_FRENO;
        }
        // La pérdida es proporcional al tiempo: el resultado no depende de la tasa de cuadros.
        velocidad *= (float) Math.exp(-resistencia * deltaTime);
        velocidad = Math.max(VELOCIDAD_MAXIMA_REVERSA, Math.min(VELOCIDAD_MAXIMA, velocidad));

        float direccion = 0;
        if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) {
            direccion += 1;
        }
        if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) {
            direccion -= 1;
        }
        // Girar en marcha atrás invierte el sentido del giro, como en un automóvil real.
        angulo += direccion * velocidad * GIRO_POR_VELOCIDAD * deltaTime;

        float frenteX = -(float) Math.sin(angulo); // Componente X del frente: cero cuando no hay giro.
        float frenteZ = -(float) Math.cos(angulo);
        float siguienteX = autoX + frenteX * velocidad * deltaTime;
        float siguienteZ = autoZ + frenteZ * velocidad * deltaTime;
        if (puedeCircular(siguienteX, siguienteZ) && !chocaConOtroVehiculo(siguienteX, siguienteZ)) {
            autoX = siguienteX;
            autoZ = siguienteZ;
        } else {
            velocidad = 0; // El choque detiene el vehículo sin moverlo: no hay rebote.
        }
    }

    // ==================== 4. COLISIONES CON OTROS VEHÍCULOS ====================

    /**
     * Indica si otro vehículo del escenario ocupa esa posición.
     *
     * <p>En esta etapa solo hay un coche, así que nunca hay ninguno. La etapa que
     * incorpora tráfico autónomo lo sobrescribe, igual que hace con la noche o con la
     * decoración de las manzanas. El resultado para el jugador es el de un edificio: si
     * no cabe, frena sin moverse.
     */
    protected boolean chocaConOtroVehiculo(float x, float z) {
        return false;
    }

    // ==================== 5. COLISIONES CON LA CIUDAD ====================

    /** Indica si el círculo del auto cabe en esa posición sin tocar manzanas ni bordes. */
    protected boolean puedeCircular(float x, float z) {
        float limitePermitido = LIMITE - RADIO_AUTO; // Deja sitio para que el vehículo quede dentro del mapa.
        if (Math.abs(x) > limitePermitido || Math.abs(z) > limitePermitido) {
            return false;
        }
        float mitad = CELDA / 2; // Cada manzana se trata como un cuadrado de diez unidades de lado.
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                if (MAPA[fila][columna] == 0) {
                    continue; // Las celdas de calle no son obstáculo.
                }
                float centroX = centro(columna);
                float centroZ = centro(fila);
                float cercaX = Math.max(centroX - mitad, Math.min(x, centroX + mitad));
                float cercaZ = Math.max(centroZ - mitad, Math.min(z, centroZ + mitad));
                float distanciaX = x - cercaX;
                float distanciaZ = z - cercaZ;
                float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ;
                if (distanciaCuadrada < RADIO_AUTO * RADIO_AUTO) { // Pitágoras sin raíz: más rápido.
                    return false;
                }
            }
        }
        return true;
    }

    // ==================== 6. CÁMARAS ====================

    /** Alterna entre la cámara que sigue al auto y la vista aérea de toda la ciudad. */
    @Override
    protected void configurarCamara() {
        if (camaraAerea) {
            super.configurarCamara(); // Vista general definida en Ciudad.
            return;
        }
        float camaraX = autoX + (float) Math.sin(angulo) * DISTANCIA_CAMARA; // Detrás del auto en X.
        float camaraZ = autoZ + (float) Math.cos(angulo) * DISTANCIA_CAMARA; // Detrás del auto en Z.
        vector("uOjo", camaraX, ALTURA_CAMARA, camaraZ);
        vector("uObjetivo", autoX, 0.8f, autoZ); // Mira a la carrocería, no al suelo.
        decimal("uAspecto", (float) ancho / alto);
    }

    // ==================== 7. DIBUJO DEL AUTO ====================

    /** Dibuja la ciudad heredada y encima el vehículo, cuyo aspecto vive en la clase Auto. */
    @Override
    protected void escena() {
        super.escena();
        new Auto(this, autoX, autoZ, angulo).dibujar();
    }

    // ==================== 8. CONTROLES E INDICADORES ====================

    @Override
    protected String rotulo() {
        return "PARTE 3 | CONDUCCION";
    }

    @Override
    protected String ayuda() {
        return "WASD/flechas: conducir | Espacio: freno | C: camara | R: reiniciar";
    }

    @Override
    protected String indicadores() {
        int kilometrosPorHora = Math.round(Math.abs(velocidad) * 3.6f); // Una unidad = un metro.
        String marcha = velocidad >= 0 ? "adelante" : "atras";
        return super.indicadores() + " | " + kilometrosPorHora + " km/h " + marcha;
    }
}