package grafica.ciudad; // Paquete del proyecto.

import java.util.HashMap;
import java.util.Map;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*; // Ventana, teclado y constantes.
import static org.lwjgl.opengl.GL33.*; // OpenGL hasta la versión 3.3.

/**
 * BASE DE LA APLICACIÓN: MECÁNICA DE OPENGL.
 * Clase abstracta de la que heredan Ciudad, Conduccion, Iluminacion y
 * CiudadCompleta. Solo contiene la mecánica de OpenGL: ventana, ciclo de dibujo,
 * cubo unitario, envío de variables a la GPU y shaders. No dibuja ciudad ni
 * simula nada; esos dos papeles los dan las etapas que heredan de ella.
 * Coordenadas del mundo: X = izquierda/derecha, Y = altura, Z = profundidad.
 */
public abstract class Motor {

    // ==================== 1. VARIABLES DE LA APLICACIÓN ====================

    protected long ventana;
    protected int programa;
    protected int vao;
    protected int vbo;
    protected int ancho = 1100; // Tras el arranque contiene los píxeles reales del framebuffer.
    protected int alto = 760;
    protected static final float ORBITA_AEREA = 0.6f; // Ángulo fijo de la cámara aérea, en radianes.
    protected float tiempo = 0; // Reloj de la simulación en segundos, compartido por las etapas.
    protected boolean vistaMapa = false; // Lo activan las etapas finales al dibujar el minimapa.
    private final Map<String, Integer> uniforms = new HashMap<>(); // Caché de ubicaciones de los uniforms.

    // ==================== 2. INICIO, CICLO Y LIMPIEZA ====================

    /** Organiza las tres fases de la aplicación y garantiza la limpieza si algo falla. */
    public void run() {
        GLFWErrorCallback errores = GLFWErrorCallback.createPrint(System.err);
        errores.set();
        try { // El bloque finally se ejecuta igual si la aplicación falla a mitad del ciclo.
            iniciar();
            loop();
        } finally {
            limpiar();
            glfwSetErrorCallback(null); // Desconecta el manejador antes de liberar su memoria.
            errores.free();
        }
    }

    /** Configura GLFW y OpenGL, igual que los ejemplos del proyecto de referencia. */
    private void iniciar() {
        if (!glfwInit()) {
            throw new IllegalStateException("No se pudo iniciar GLFW");
        }
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // Compatibilidad con los contextos modernos de macOS.
        ventana = glfwCreateWindow(ancho, alto, titulo(), 0, 0);
        if (ventana == 0) {
            throw new IllegalStateException("No se pudo crear la ventana OpenGL");
        }
        glfwMakeContextCurrent(ventana);
        glfwSwapInterval(1); // Sincroniza la presentación con la pantalla para evitar cortes de imagen.
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST); // Las superficies cercanas ocultan a las lejanas.
        glfwSetKeyCallback(ventana, (ventanaEvento, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) { // Una sola vez al presionar, no en cada repetición de la tecla.
                tecla(key); // Cada etapa amplía este método con super.
            }
        });
        crearPrograma();
        crearCubo();
    }

    /** Mantiene el ciclo continuo de eventos, actualización, dibujo y presentación. */
    private void loop() {
        double tiempoAnterior = glfwGetTime();
        int cuadros = 0;
        int maxCuadros = Integer.getInteger("demo.frames", 0); // Cero deja jugar; otro valor cierra la ventana solo.
        int[] anchoReal = new int[1]; // GLFW escribe aquí los píxeles reales, también en pantallas Retina.
        int[] altoReal = new int[1];

        while (!glfwWindowShouldClose(ventana)) {
            glfwPollEvents();
            double tiempoActual = glfwGetTime();
            float deltaTime = (float) Math.min(tiempoActual - tiempoAnterior, 0.05); // Evita saltos de más de 50 ms.
            tiempoAnterior = tiempoActual;
            tiempo += deltaTime; // El reloj avanza siempre, aunque la etapa todavía no lo use.
            actualizar(deltaTime);
            actualizarTitulo();
            glfwGetFramebufferSize(ventana, anchoReal, altoReal);
            ancho = anchoReal[0];
            alto = altoReal[0];
            if (ancho > 0 && alto > 0) { // Con la ventana minimizada no hay nada que dibujar.
                dibujarFrame();
            }
            glfwSwapBuffers(ventana);
            cuadros++;
            if (maxCuadros > 0 && cuadros >= maxCuadros) {
                glfwSetWindowShouldClose(ventana, true);
            }
        }
    }

    /** Libera únicamente los recursos que llegaron a crearse. */
    private void limpiar() {
        if (programa != 0) {
            glDeleteProgram(programa);
        }
        if (vbo != 0) {
            glDeleteBuffers(vbo);
        }
        if (vao != 0) {
            glDeleteVertexArrays(vao);
        }
        if (ventana != 0) {
            Callbacks.glfwFreeCallbacks(ventana);
            glfwDestroyWindow(ventana); // Destruye la ventana y su contexto OpenGL.
        }
        glfwTerminate();
    }

    // ==================== 3. TECLADO, CÁMARA Y TÍTULO ====================

    /** Atiende las acciones que ocurren una sola vez por pulsación. */
    protected void tecla(int key) {
        if (key == GLFW_KEY_ESCAPE) {
            glfwSetWindowShouldClose(ventana, true);
        }
    }

    /** Consulta si una tecla sigue presionada durante el cuadro actual. */
    protected boolean pulsada(int key) {
        return glfwGetKey(ventana, key) == GLFW_PRESS;
    }

    /** Actualiza la etapa; recibe los segundos transcurridos desde el cuadro anterior. */
    protected void actualizar(float deltaTime) {
        // El motor no simula nada: la conducción de Conduccion sustituye este método.
    }

    /** Nombre de la etapa en la barra de la ventana; cada etapa devuelve el suyo. */
    protected abstract String rotulo();

    /** Lista de controles de la etapa; las siguientes clases la amplían con super. */
    protected String ayuda() {
        return "";
    }

    /** Indicadores de estado; las siguientes etapas añaden aquí luces, entregas o mapa. */
    protected abstract String indicadores();

    /** Compone el título con el nombre de la etapa, sus controles y sus indicadores. */
    private String titulo() {
        String texto = rotulo();
        texto += " | " + ayuda();
        texto += indicadores();
        return texto;
    }

    /** Publica el título compuesto en la barra de la ventana. */
    private void actualizarTitulo() {
        glfwSetWindowTitle(ventana, titulo());
    }

    /** Coloca la cámara elevada y orientada hacia el origen de la escena. */
    protected abstract void configurarCamara();

    /** Punto de extensión para el envío de luces; la etapa de iluminación lo reemplaza. */
    protected void prepararLuces() {
        // El esqueleto usa colores planos y todavía no necesita enviar ninguna luz.
    }

    /** Prepara OpenGL y dibuja una imagen completa. */
    protected void dibujarFrame() {
        glViewport(0, 0, ancho, alto);
        glClearColor(0.12f, 0.20f, 0.30f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(programa);
        glBindVertexArray(vao);
        entero("uMapa", 0); // Elige la proyección en perspectiva, no la del minimapa ni la del panel.
        decimal("uSemimapa", 1.0f); // Provisional: la etapa final enviará el tamaño real de la ciudad.
        vector2("uPantalla", ancho, alto); // Necesario para colocar el panel en píxeles.
        configurarCamara();
        prepararLuces(); // La etapa de iluminación sobrescribe este método.
        escena();
        if (glGetError() != GL_NO_ERROR) {
            throw new IllegalStateException("Error OpenGL al dibujar");
        }
    }

    /** Dibuja la escena de la etapa; Ciudad la sustituye por el mapa y sus manzanas. */
    protected abstract void escena();

    // ==================== 4. DIBUJAR CAJAS Y ENVIAR UNIFORMS ====================

    /** Dibuja una caja sin giro: posición XYZ, tamaño XYZ y color RGB entre 0 y 1. */
    public void caja(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        cajaGirada(x, y, z, sx, sy, sz, r, g, b, 0);
    }

    /** Dibuja el cubo unitario escalado, girado alrededor de Y, trasladado y coloreado. */
    public void cajaGirada(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b, float angulo) {
        vector("uPos", x, y, z); // Centro de la caja, en el mundo o en el panel.
        vector("uEscala", sx, sy, sz); // Ancho, altura y profundidad.
        vector("uColor", r, g, b);
        decimal("uGiro", angulo); // Orientación en radianes alrededor del eje vertical.
        glDrawArrays(GL_TRIANGLES, 0, 36); // Doce triángulos: dos por cada una de las seis caras.
    }

    /** Busca un uniform una sola vez y guarda su ubicación para los siguientes dibujos. */
    public int uniform(String nombre) {
        if (!uniforms.containsKey(nombre)) {
            int ubicacion = glGetUniformLocation(programa, nombre); // El valor menos uno indica que el shader no la usa.
            uniforms.put(nombre, ubicacion);
        }
        return uniforms.get(nombre); // OpenGL ignora los envíos a un uniform inexistente.
    }

    /** Envía tres números reales a una variable de tipo vec3 del shader. */
    public void vector(String nombre, float x, float y, float z) {
        glUniform3f(uniform(nombre), x, y, z);
    }

    /** Envía dos números reales a una variable de tipo vec2 del shader. */
    public void vector2(String nombre, float x, float y) {
        glUniform2f(uniform(nombre), x, y);
    }

    /** Envía un número real a una variable de tipo float del shader. */
    public void decimal(String nombre, float valor) {
        glUniform1f(uniform(nombre), valor);
    }

    /** Envía un número entero a una variable de tipo int del shader. */
    public void entero(String nombre, int valor) {
        glUniform1i(uniform(nombre), valor); // También se usa como interruptor 0 o 1.
    }

    /** Dibuja una caja que brilla por sí misma, sin recibir luz de ninguna fuente. */
    public void cajaEmisiva(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        entero("uEmision", 1);
        caja(x, y, z, sx, sy, sz, r, g, b);
        entero("uEmision", 0);
    }

    /** Igual que cajaEmisiva(), pero con giro alrededor del eje vertical. */
    public void cajaGiradaEmisiva(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b, float angulo) {
        entero("uEmision", 1);
        cajaGirada(x, y, z, sx, sy, sz, r, g, b, angulo);
        entero("uEmision", 0);
    }

    /**
     * Mantiene el material emisivo durante el dibujo y lo restaura pase lo que pase.
     * Existe para que ninguna construcción tenga que acordarse de cerrar el interruptor
     * a mano: si lo olvidara, todo lo que se dibujase después en el mismo cuadro brillaría.
     */
    public void conEmision(Runnable dibujo) {
        entero("uEmision", 1);
        try {
            dibujo.run();
        } finally {
            entero("uEmision", 0);
        }
    }

    // ==================== 5. SHADERS: CÓDIGO QUE EJECUTA LA GPU ====================

    /** Transforma cada vértice del cubo al mundo y de ahí a la pantalla; admite tres proyecciones. */
    private String vertexShader() {
        return """
            #version 330 core // GLSL 3.30 para el contexto OpenGL 3.3.
            layout (location = 0) in vec3 aPos; // Posición local del vértice, leída del VBO.
            layout (location = 1) in vec3 aNormal; // Normal de la cara, leída del mismo VBO.
            uniform vec3 uPos; // Centro de la caja en la ciudad.
            uniform vec3 uEscala; // Tamaño de la caja en cada eje.
            uniform vec3 uOjo; // Posición de la cámara.
            uniform vec3 uObjetivo; // Punto que observa la cámara.
            uniform float uGiro; // Giro del objeto alrededor del eje vertical.
            uniform float uAspecto; // Relación entre el ancho y el alto de la imagen.
            uniform int uMapa; // Proyección: 0 perspectiva, 1 minimapa, 2 panel de pantalla.
            uniform float uSemimapa; // Medio lado de la ciudad para encuadrar el minimapa.
            uniform vec2 uPantalla; // Tamaño en píxeles del área de dibujo, para el panel.
            out vec3 vMundo; // Posición mundial para el shader de fragmentos.
            out vec3 vNormal; // Normal transformada para el cálculo de iluminación.

            void main() { // OpenGL ejecuta este bloque una vez por cada vértice.
                float coseno = cos(uGiro);
                float seno = sin(uGiro);
                mat3 giro = mat3(coseno, 0.0, -seno, // Columna del eje X ya rotado.
                                 0.0, 1.0, 0.0,        // El eje Y permanece vertical.
                                 seno, 0.0, coseno);    // Columna del eje Z ya rotado.
                vec3 local = giro * (aPos * uEscala); // Aplica tamaño y giro, todavía sin trasladar.
                vMundo = local + uPos;
                vNormal = normalize(giro * (aNormal / uEscala)); // Corrige la normal con la inversa de la escala.

                if (uMapa == 1) { // Minimapa: vista superior sin perspectiva.
                    float pantallaX = vMundo.x / uSemimapa; // Encaja el ancho del mundo entre menos uno y uno.
                    float pantallaY = -vMundo.z / uSemimapa; // El norte, que es menos Z, queda arriba.
                    float profundidad = -vMundo.y / 100.0; // Lo más alto se dibuja encima.
                    gl_Position = vec4(pantallaX, pantallaY, profundidad, 1.0);
                } else if (uMapa == 2) { // Panel: la posición llega en píxeles, con la Y hacia arriba.
                    vec2 centro = uPos.xy + local.xy;
                    gl_Position = vec4(centro.x / uPantalla.x * 2.0 - 1.0,
                                       centro.y / uPantalla.y * 2.0 - 1.0, 0.0, 1.0);
                } else { // Escena principal: cámara con perspectiva.
                    vec3 frente = normalize(uObjetivo - uOjo);
                    vec3 derecha = normalize(cross(frente, vec3(0.0, 1.0, 0.0))); // Eje horizontal de la cámara.
                    vec3 arriba = cross(derecha, frente); // Eje vertical, perpendicular a los otros dos.
                    vec3 diferencia = vMundo - uOjo; // Traslada el origen del mundo hasta la cámara.
                    float vistaX = dot(diferencia, derecha);
                    float vistaY = dot(diferencia, arriba);
                    float vistaZ = -dot(diferencia, frente); // Z negativa delante de la cámara, como espera OpenGL.
                    float factor = 1.0 / tan(radians(55.0) * 0.5); // Campo visual de 55 grados convertido en escala.
                    float cerca = 0.1; // Distancia mínima visible.
                    float lejos = 250.0; // Distancia máxima visible.
                    float recorteX = vistaX * factor / uAspecto; // Corrige el ancho según el de la pantalla.
                    float recorteY = vistaY * factor;
                    float recorteZ = (lejos + cerca) / (cerca - lejos) * vistaZ;
                    recorteZ += 2.0 * lejos * cerca / (cerca - lejos); // Término constante de profundidad.
                    gl_Position = vec4(recorteX, recorteY, recorteZ, -vistaZ); // W negativo crea la perspectiva.
                }
            }
            """; // Termina la cadena que contiene el shader de vértices.
    }

    /** Pinta cada fragmento con el color plano del material; la etapa de iluminación lo reemplaza. */
    protected String fragmentShader() {
        return """
            #version 330 core
            uniform vec3 uColor; // Color RGB enviado por cajaGirada().
            out vec4 color; // Color final que se escribe en la imagen.
            void main() { // Se ejecuta para cada fragmento de la geometría dibujada.
                color = vec4(uColor, 1.0); // Material con opacidad completa.
            }
            """; // Termina la cadena del shader de fragmentos.
    }

    /** Compila un shader y comunica a la consola el mensaje del controlador si hay un error GLSL. */
    private int compilar(int tipo, String fuente) {
        int shader = glCreateShader(tipo);
        glShaderSource(shader, fuente);
        glCompileShader(shader); // La compilación depende de la tarjeta gráfica.
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String error = glGetShaderInfoLog(shader); // Se lee antes de borrar el objeto.
            glDeleteShader(shader);
            throw new IllegalStateException(error); // Diagnóstico del controlador, sin más comentarios.
        }
        return shader; // Devuelve el identificador del shader compilado.
    }

    /** Une los shaders en un programa ejecutable y libera los objetos intermedios. */
    private void crearPrograma() {
        int shaderVertices = compilar(GL_VERTEX_SHADER, vertexShader());
        int shaderFragmentos = 0; // Permite saber si el segundo shader llegó a crearse.
        try { // El finally libera ambos objetos aunque el programa falle al enlazar.
            shaderFragmentos = compilar(GL_FRAGMENT_SHADER, fragmentShader());
            programa = glCreateProgram();
            glAttachShader(programa, shaderVertices);
            glAttachShader(programa, shaderFragmentos);
            glLinkProgram(programa);
            if (glGetProgrami(programa, GL_LINK_STATUS) == GL_FALSE) {
                throw new IllegalStateException(glGetProgramInfoLog(programa));
            }
        } finally {
            glDeleteShader(shaderVertices);
            if (shaderFragmentos != 0) {
                glDeleteShader(shaderFragmentos);
            }
        }
    }

    // ==================== 6. GEOMETRÍA DEL CUBO: VAO Y VBO ====================

    /** Crea el cubo explícito con posición XYZ y normal XYZ por vértice. */
    private void crearCubo() {
        float[] vertices = { // Cada vértice: X, Y, Z, normalX, normalY, normalZ. Seis vértices por cara.
            -0.5f, -0.5f, 0.5f, 0, 0, 1, // Cara frontal, normal hacia más Z: dos triángulos.
            0.5f, -0.5f, 0.5f, 0, 0, 1,
            0.5f, 0.5f, 0.5f, 0, 0, 1,
            0.5f, 0.5f, 0.5f, 0, 0, 1,
            -0.5f, 0.5f, 0.5f, 0, 0, 1,
            -0.5f, -0.5f, 0.5f, 0, 0, 1,

            -0.5f, -0.5f, -0.5f, 0, 0, -1, // Cara trasera, normal hacia menos Z.
            -0.5f, 0.5f, -0.5f, 0, 0, -1,
            0.5f, 0.5f, -0.5f, 0, 0, -1,
            0.5f, 0.5f, -0.5f, 0, 0, -1,
            0.5f, -0.5f, -0.5f, 0, 0, -1,
            -0.5f, -0.5f, -0.5f, 0, 0, -1,

            -0.5f, 0.5f, 0.5f, -1, 0, 0, // Cara izquierda, normal hacia menos X.
            -0.5f, 0.5f, -0.5f, -1, 0, 0,
            -0.5f, -0.5f, -0.5f, -1, 0, 0,
            -0.5f, -0.5f, -0.5f, -1, 0, 0,
            -0.5f, -0.5f, 0.5f, -1, 0, 0,
            -0.5f, 0.5f, 0.5f, -1, 0, 0,

            0.5f, 0.5f, 0.5f, 1, 0, 0, // Cara derecha, normal hacia más X.
            0.5f, -0.5f, 0.5f, 1, 0, 0,
            0.5f, -0.5f, -0.5f, 1, 0, 0,
            0.5f, -0.5f, -0.5f, 1, 0, 0,
            0.5f, 0.5f, -0.5f, 1, 0, 0,
            0.5f, 0.5f, 0.5f, 1, 0, 0,

            -0.5f, 0.5f, -0.5f, 0, 1, 0, // Cara superior, normal hacia más Y.
            -0.5f, 0.5f, 0.5f, 0, 1, 0,
            0.5f, 0.5f, 0.5f, 0, 1, 0,
            0.5f, 0.5f, 0.5f, 0, 1, 0,
            0.5f, 0.5f, -0.5f, 0, 1, 0,
            -0.5f, 0.5f, -0.5f, 0, 1, 0,

            -0.5f, -0.5f, -0.5f, 0, -1, 0, // Cara inferior, normal hacia menos Y.
            0.5f, -0.5f, -0.5f, 0, -1, 0,
            0.5f, -0.5f, 0.5f, 0, -1, 0,
            0.5f, -0.5f, 0.5f, 0, -1, 0,
            -0.5f, -0.5f, 0.5f, 0, -1, 0,
            -0.5f, -0.5f, -0.5f, 0, -1, 0 // Fin de los treinta y seis vértices del cubo.
        };
        vao = glGenVertexArrays(); // Recuerda la disposición de los atributos.
        glBindVertexArray(vao);
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW); // El cubo no cambia nunca.
        int bytesPorVertice = 6 * Float.BYTES; // Seis números reales de cuatro bytes por vértice.
        glVertexAttribPointer(0, 3, GL_FLOAT, false, bytesPorVertice, 0L); // Posición desde el primer byte.
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, bytesPorVertice, 3L * Float.BYTES); // Normal después de XYZ.
        glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }
}