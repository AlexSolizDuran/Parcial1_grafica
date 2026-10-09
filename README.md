# Ciudad interactiva ampliada

Ciudad 3D en Java con LWJGL y OpenGL 3.3. El mapa original de 7x7 celdas se amplía a
11x11 conservando el tamaño de celda, con iluminación día/noche, decoración urbana,
semáforos animados, minimapa, entregas y tráfico autónomo.

Todo se genera con código a partir de un único cubo unitario de 36 vértices: sin
imágenes, modelos, tipografías ni sonidos.

## Requisitos

- JDK 17 o superior
- Maven
- Un controlador gráfico con OpenGL 3.3 en modo *core*

## Ejecutar

La versión final es `CiudadCompleta`. Desde este directorio:

```sh
mvn compile exec:exec
```

Cada etapa tiene su propia clase `main` y se lanza indicando cuál:

```sh
mvn compile exec:exec -DmainClass=grafica.ciudad.Motor           # parte 1, cubo de prueba
mvn compile exec:exec -DmainClass=grafica.ciudad.Ciudad          # parte 2, la ciudad
mvn compile exec:exec -DmainClass=grafica.ciudad.Conduccion      # parte 3, conducción
mvn compile exec:exec -DmainClass=grafica.ciudad.Iluminacion      # parte 4, luces
mvn compile exec:exec -DmainClass=grafica.ciudad.CiudadCompleta   # parte 5, versión final
```

Para una comprobación gráfica corta, añade `-Ddemo.frames=N`: la ventana se dibuja
durante `N` cuadros y se cierra sola. Es lo que sirve para comprobar que los shaders
compilan y que no hay errores de OpenGL.

```sh
mvn compile exec:exec -Ddemo.frames=20
```

Pruebas de lógica, sin abrir ventana:

```sh
mvn test
```

Para empaquetar un jar único con todas las dependencias dentro:

```sh
mvn package
java -jar target/ciudad-1.0.0.jar
```

En macOS GLFW necesita ejecutarse en el primer hilo del proceso Java. El `pom.xml` lo
resuelve con un perfil que se activa solo en Mac; si ejecutas desde un IDE en Mac,
añade `-XstartOnFirstThread` a las opciones de la JVM a mano. Los detalles están en la
sección siguiente.

## Ejecutar en macOS

macOS necesita dos cosas que Linux no: GLFW debe correr en el primer hilo del proceso
Java, y hay que descargar los binarios nativos de LWJGL para macOS. El `pom.xml` ya
trae un perfil que resuelve ambas cosas, así que desde la terminal los comandos son los
mismos de siempre.

### Desde la terminal

```sh
mvn compile exec:exec
```

El perfil `macos` del `pom.xml` se activa solo (`<os><family>mac</family></os>`) y
añade por su cuenta el argumento `-XstartOnFirstThread` y las dependencias de binarios
nativos. No hay que escribir nada distinto en el comando.

Para las demás etapas y para la comprobación corta:

```sh
mvn compile exec:exec -DmainClass=grafica.ciudad.Ciudad
mvn compile exec:exec -Ddemo.frames=20
```

Y las pruebas, que no abren ventana y no necesitan nada especial:

```sh
mvn test
```

### Desde un IDE

El perfil solo afecta a Maven. Si lanzas la clase desde IntelliJ, Eclipse o VS Code
tienes que añadir el argumento a mano:

- **IntelliJ**: `Run` → `Edit Configurations` → en `VM options`, `-XstartOnFirstThread`
- **Eclipse**: `Run Configurations` → `Arguments` → `VM arguments`, `-XstartOnFirstThread`
- **VS Code**: en `launch.json`, en `"vmArgs"`, `"-XstartOnFirstThread"`

### Desde el jar empaquetado

El perfil no cubre el jar, porque ahí el comando es `java` y no Maven. El argumento
hay que ponerlo a mano:

```sh
mvn package
java -XstartOnFirstThread -jar target/ciudad-1.0.0.jar
```

Sin ese argumento, GLFW aborta con un aviso del tipo
`Exception in thread "main" java.lang.IllegalStateException: GLFW is not initialized`
o bien falla al inicializar la biblioteca.

### Apple Silicon

El perfil declara los dos juegos de binarios, `natives-macos` (Intel, `macos/x64`) y
`natives-macos-arm64` (Apple Silicon, `macos/arm64`). LWJGL elige la carpeta que
corresponde a la arquitectura en tiempo de carga, así que el mismo comando funciona en
los dos tipos de Mac sin cambiar nada.

### Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| `UnsatisfiedLinkError` al arrancar | Faltan los binarios nativos de macOS | Comprueba que se está usando Maven y no el classpath del IDE, para que el perfil añada las dependencias |
| `GLFW is not initialized` | GLFW está en un hilo que no es el principal | Añade `-XstartOnFirstThread` a las opciones de la JVM |
| Ventana en blanco o negro | Controlador sin OpenGL 3.3 en modo core | Actualiza el driver; en Mac, la GPU debe tener al menos 3.3 |
| Aviso de LWJGL sobre la versión de JNI | El JDK del sistema es el 25 y el *toolchain* es el 21 | No afecta al resultado; usa el JDK 17 para silenciarlo |

En macOS el contexto se crea con `GLFW_OPENGL_FORWARD_COMPAT` activado, que es
obligatorio en los contextos modernos de Apple y por eso está puesto en el código.

## Controles

| Tecla | Acción |
|---|---|
| W / S o flechas ↑ ↓ | Acelerar, frenar y dar marcha atrás |
| A / D o flechas ← → | Girar mientras el vehículo se mueve |
| Espacio | Freno de mayor intensidad |
| C | Cámara de seguimiento / aérea oblicua |
| R | Reiniciar vehículo, tráfico y entregas |
| N | Ambiente diurno / nocturno |
| F | Encender / apagar los faros |
| M | Mostrar / ocultar el minimapa |
| ESC | Cerrar la ventana |

En la parte 2, antes de que exista el vehículo, las flechas horizontales orbitan la
cámara. El título de la ventana muestra siempre la parte, los controles y los
indicadores de estado.

La partida son tres entregas: hay que llegar a menos de 3 unidades del destino dorado
**frenando** (por debajo de 1 unidad por segundo) para completarla.

## Estructura

```text
Motor         ventana, ciclo de dibujo, cubo unitario, uniforms, shaders
  └─ Ciudad        mapa 11x11, suelo, aceras, manzanas, marcas viales, cámara aérea
       └─ Conduccion    vehículo, conducción, colisiones, cámaras, reinicio
            └─ Iluminacion    día y noche, farolas, faros, shader de luz
                 └─ CiudadCompleta  entregas, decoración, semáforos, minimapa, panel
Texto         fuente de mapa de bits 3x5 para el panel
Trafico       los tres coches autónomos
construcciones/  Edificio, Auto, Farola, Parque, Semaforo
```

Cada etapa reutiliza la anterior con `super` y añade una sola responsabilidad.

## La ciudad

| | Celdas | Mínimo pedido |
|---|---|---|
| Manzanas con edificio | 18 | 12 |
| Parques | 7 | 4 |
| Celdas de calle | 96 | — |
| Farolas | 10 | 9 |

Cada celda mide 10 unidades, así que la ciudad ocupa 110 x 110 con el centro en el
origen. Las calles ocupan los índices pares de filas y columnas y las manzanas los
impares, de modo que la red vial queda conectada y toda manzana está rodeada de calle.
El 7x7 original se conserva intacto en la esquina noroeste.

## Cómo se dibuja

Todo sale de un solo cubo de 36 vértices cargado una vez en la GPU (864 bytes). Cada
pieza envía cuatro uniforms y lanza un dibujado:

```java
vector("uPos",   x, y, z);         // dónde se coloca
vector("uEscala", sx, sy, sz);     // qué tamaño tiene
vector("uColor",  r, g, b);        // de qué color se pinta
decimal("uGiro",  angulo);         // hacia dónde mira
glDrawArrays(GL_TRIANGLES, 0, 36); // siempre los mismos 36 vértices
```

Un vehículo son diez cajas, un edificio un cuerpo más sus ventanas, y hasta las
letras del panel son cubitos. La memoria de geometría no crece con el tamaño del
mundo.

Se crea un solo VAO y un solo VBO. El VAO recuerda cómo leer el cubo y se enlaza una
vez por cuadro: como en OpenGL lo caro es cambiar el estado y no dibujar, todos los
cambios de estado se concentran al principio del cuadro y después solo hay dibujados.

Un uniform entero selecciona una de tres proyecciones en el mismo shader: `0` la
escena con perspectiva, `1` el minimapa ortográfico y `2` el panel en píxeles.

## Pruebas

`mvn test` ejecuta 38 pruebas sin ventana, repartidas en cinco clases:

| Clase | Pruebas |
|---|---|
| `CiudadTest` | 7 |
| `ConduccionTest` | 6 |
| `IluminacionTest` | 5 |
| `CiudadCompletaTest` | 6 |
| `TraficoTest` | 14 |

La más exigente simula 18000 cuadros y comprueba en cada cuadro que ningún coche
entra en una manzana, se sale del mapa o se solapa con otro o con el jugador.

## Limitaciones

- La iluminación es local: no hay sombras ni oclusión, así que una farola ilumina a
  través de un edificio.
- Los semáforos son decorativos y el tráfico no se detiene en ellos.
- El choque detiene el vehículo sin rebote: si quedas encajado, usa `R`.
- La rotación de las piezas es de un solo eje, el vertical.
- No hay instanciación: cada pieza es una llamada de dibujado propia.
- La fuente del panel se dibuja con cajas, una por punto encendido. Es el punto con
  más coste del proyecto.
- Sin librería de matrices, el campo de visión queda fijo en 55 grados.
- En este equipo el arranque muestra un aviso de LWJGL sobre la versión de JNI al
  ejecutarse con el JDK 25, aunque la compilación usa el JDK 21 del *toolchain*. No
  afecta al resultado.

## Documentación

La memoria de entrega con el diseño, las decisiones técnicas y el detalle de cada
funcionamiento está en `Ciudad-interactiva-Memoria.docx`.