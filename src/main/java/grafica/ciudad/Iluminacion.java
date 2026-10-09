package grafica.ciudad;

import grafica.ciudad.construcciones.Auto;
import grafica.ciudad.construcciones.Farola;
import static org.lwjgl.glfw.GLFW.*;


/**
 * PARTE 4: ILUMINACIÓN DE LA CIUDAD.
 * Hereda de Conduccion la ciudad y la conducción, y añade luz: ambiente diurno y
 * nocturno con N, dieciséis farolas repartidas por los seis cruces de la ciudad
 * ampliada, y dos focos delanteros en el vehículo que se encienden con F.
 * El shader calcula la luz difusa a partir de las normales; no hay sombras ni oclusión,
 * así que una luz puede atravesar un edificio. La bombilla que brilla y la luz que
 * recibe el entorno son dos cosas distintas y se controlan por separado.
 */
public class Iluminacion extends Conduccion {

    // ==================== 1. ESTADO DE LAS LUCES ====================

    protected static final float ALCANCE_FAROLA = 22; // A partir de aquí una farola ya no ilumina.
    protected static final float ALCANCE_FARO = 26; // Alcance de cada foco delantero.
    protected static final float APERTURA_FARO = 0.85f; // Coseno del borde exterior del cono.
    protected static final float CENTRO_FARO = 0.97f; // Coseno del centro del cono, más estrecho y brillante.

    protected boolean noche = false; // Arranca de día para que la ciudad se lea de golpe; N alterna.
    protected boolean faros = true; // Los focos del auto empiezan encendidos para poder conducir de noche.

    // ==================== 2. CONTROLES E INDICADORES ====================

    /** La etapa de luces es la que sabe si es de día o de noche. */
    @Override
    protected boolean esNoche() {
        return noche;
    }

    /** Añade N y F a los controles heredados. */
    @Override
    protected void tecla(int key) {
        super.tecla(key); // Conserva ESC, C, R y la conducción.
        if (key == GLFW_KEY_N) {
            noche = !noche;
        }
        if (key == GLFW_KEY_F) {
            faros = !faros;
        }
    }

    @Override
    protected String rotulo() {
        return "PARTE 4 | ILUMINACION";
    }

    @Override
    protected String ayuda() {
        return super.ayuda() + " | N: dia/noche | F: faros";
    }

    @Override
    protected String indicadores() {
        String ambiente = noche ? "noche" : "dia";
        String estadoFaros = faros ? "ON" : "OFF";
        return super.indicadores() + " | N: " + ambiente + " | F: faros " + estadoFaros;
    }

    // ==================== 3. DATOS QUE RECIBE EL SHADER ====================

    /** Envía el estado de las luces antes de dibujar la escena. */
    @Override // Sustituye el método vacío de Motor.
    protected void prepararLuces() {
        entero("uNoche", noche ? 1 : 0);
        entero("uFaros", faros ? 1 : 0);
        entero("uEmision", 0); // Por defecto ningún objeto brilla por sí mismo.
        for (int indice = 0; indice < Farola.POSICIONES.length; indice++) {
            vector("uLuces[" + indice + "]", Farola.POSICIONES[indice][0], Farola.POSICIONES[indice][1],
                    Farola.POSICIONES[indice][2]);
        }
        // El foco de cada faro se sitúa delante de la carrocería y a los lados del vehículo.
        vector("uAuto", autoX, 0.7f, autoZ);
        vector("uFrente", -(float) Math.sin(angulo), 0, -(float) Math.cos(angulo));
    }

    // ==================== 4. FAROLAS Y HALOS DEL VEHÍCULO ====================

    /** Dibuja la ciudad y el auto heredados, más las dieciséis farolas y el halo de los faros. */
    @Override
    protected void escena() {
        super.escena(); // Ciudad, parcelas y vehículo de las etapas anteriores.
        Farola.dibujarTodas(this, noche);
        if (faros) {
            // El halo del faro es emisión pura, no luz recibida, y depende de la etapa de luces.
            new Auto(this, autoX, autoZ, angulo).dibujarHalos();
        }
    }

    // ==================== 5. CÁLCULO DE LA LUZ EN LA GPU ====================

    /** Devuelve el programa GLSL que colorea cada fragmento según las luces de la escena. */
    @Override // Sustituye el color plano de Motor.
    protected String fragmentShader() {
        return fragmentShader(Farola.POSICIONES.length);
    }

    /**
     * El shader recibe cuántas farolas hay. Así la forma del programa no depende de
     * quién declare las farolas: añadir una luz es cambiar el array de Farola, no el GLSL.
     */
    protected String fragmentShader(int numeroLuces) {
        return """
            #version 330 core
            in vec3 vMundo; // Posición del fragmento dentro de la ciudad.
            in vec3 vNormal; // Dirección perpendicular a la superficie.
            uniform vec3 uColor; // Color base de la caja.
            uniform vec3 uLuces[%d]; // Posiciones de las bombillas de la ciudad.
            uniform vec3 uAuto; // Centro del vehículo a la altura de sus faros.
            uniform vec3 uFrente; // Dirección hacia la que apunta el vehículo.
            uniform int uNoche; // Uno de noche, cero de día.
            uniform int uFaros; // Uno cuando los focos están encendidos.
            uniform int uEmision; // Uno si el objeto debe brillar sin recibir luz.
            uniform int uMapa; // Uno durante el minimapa, dos durante el panel.
            out vec4 color; // Color final que se escribe en la imagen.

            void main() { // Se ejecuta para cada fragmento visible de una caja.
                if (uEmision == 1 || uMapa != 0) {
                    // Bombillas, minimapa y panel usan su color directo: el mapa no se ilumina.
                    color = vec4(uColor, 1.0);
                    return;
                }

                vec3 normal = normalize(vNormal);
                vec3 luz = vec3(0.48); // Luz ambiental diurna, presente en todas las caras.
                float intensidadSol = 0.65;
                if (uNoche == 1) {
                    luz = vec3(0.12, 0.16, 0.24); // Ambiente nocturno, tenue y azulado.
                    intensidadSol = 0.10; // El sol se sustituye por la luna.
                }

                // Sol o luna: aporte direccional que depende de la orientación de la cara.
                vec3 direccionSol = normalize(vec3(0.4, 1.0, 0.3));
                float incidenciaSol = max(dot(normal, direccionSol), 0.0); // Ley de Lambert.
                luz += vec3(intensidadSol) * incidenciaSol;

                if (uNoche == 1) {
                    for (int indice = 0; indice < %d; indice++) {
                        vec3 haciaLuz = uLuces[indice] - vMundo;
                        float distancia = length(haciaLuz);
                        if (distancia > %s) {
                            continue; // Descarta la bombilla: es la clave para no pagar dieciséis bucles siempre.
                        }
                        float difusa = max(dot(normal, haciaLuz / distancia), 0.0);
                        // Atenuación con la distancia: primero lineal y luego cuadrática.
                        float atenuacion = 1.0 + 0.12 * distancia + 0.045 * distancia * distancia;
                        luz += vec3(1.0, 0.73, 0.34) * difusa * 3.0 / atenuacion;
                    }
                }

                if (uFaros == 1) {
                    vec3 lateral = vec3(-uFrente.z, 0.0, uFrente.x); // Dirección hacia el costado del auto.
                    for (int indice = 0; indice < 2; indice++) {
                        float separacion = indice == 0 ? -0.55 : 0.55; // Faro izquierdo y derecho.
                        vec3 origen = uAuto + uFrente * 1.36 + lateral * separacion;
                        vec3 haciaSuperficie = vMundo - origen;
                        float distancia = length(haciaSuperficie);
                        if (distancia > %s) {
                            continue; // El foco tiene alcance limitado.
                        }
                        vec3 eje = normalize(uFrente + vec3(0.0, -0.10, 0.0)); // Ligeramente inclinado al suelo.
                        float alineacion = dot(haciaSuperficie / distancia, eje);
                        float cono = smoothstep(%s, %s, alineacion); // Suaviza el borde del haz.
                        float difusa = max(dot(normal, -haciaSuperficie / distancia), 0.0);
                        float atenuacion = 1.0 + 0.04 * distancia * distancia;
                        luz += vec3(1.0, 0.94, 0.72) * cono * difusa * 8.0 / atenuacion;
                    }
                }

                color = vec4(uColor * luz, 1.0); // Material multiplicado por toda la luz acumulada.
            }
            """.formatted(numeroLuces, numeroLuces, ALCANCE_FAROLA, ALCANCE_FARO, APERTURA_FARO, CENTRO_FARO);
    }
}