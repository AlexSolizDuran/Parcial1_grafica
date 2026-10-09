# Ciudad interactiva ampliada

Ampliación de una ciudad 3D con LWJGL y OpenGL 3.3, en Java y Maven. El mapa original
de 7x7 se amplía a 11x11 conservando el tamaño de celda, con iluminación día/noche,
decoración urbana, minimapa, entregas y panel de información dentro de la ventana.

- JDK 17 o superior y Maven.
- Sin recursos externos: la geometría, los colores y hasta la tipografía del panel se
  generan con código. No hay imágenes, modelos, fuentes ni sonidos.

## Ejecutar

```sh
mvn compile exec:exec
```

Ese comando abre la versión final (`CiudadCompleta`). Cada etapa tiene su propia clase
y su propio `main`, y se lanza cambiando el nombre de la clase:

```sh
mvn compile exec:exec -DmainClass=grafica.ciudad.Motor           # parte 1
mvn compile exec:exec -DmainClass=grafica.ciudad.Ciudad          # parte 2
mvn compile exec:exec -DmainClass=grafica.ciudad.Conduccion      # parte 3
mvn compile exec:exec -DmainClass=grafica.ciudad.Iluminacion      # parte 4
mvn compile exec:exec -DmainClass=grafica.ciudad.CiudadCompleta   # parte 5
```

Comprobación de la lógica sin abrir ventana (mapa, colisiones, entregas y contenido
del panel):

```sh
mvn test
```

Comprobación gráfica corta: añade `-Ddemo.frames=20` a cualquiera de los comandos
anteriores y la ventana se cierra sola tras veinte cuadros. Es lo que se usa para
comprobar que los shaders compilan y que no hay errores OpenGL.

En macOS el perfil de Maven añade `-XstartOnFirstThread`. Si se ejecuta desde un IDE en
Mac, hay que añadir ese argumento a las opciones de la JVM.

## Controles

| Tecla | Acción |
|---|---|
| W / S o arriba / abajo | Acelerar, frenar y dar marcha atrás |
| A / D o izquierda / derecha | Girar mientras el auto se mueve |
| Espacio | Freno |
| C | Cámara de seguimiento / aérea oblicua |
| R | Reiniciar el vehículo y las entregas |
| N | Ambiente diurno / nocturno |
| F | Encender / apagar los faros del vehículo |
| M | Mostrar / ocultar el minimapa |
| ESC | Salir |

En la parte 2 las flechas horizontales orbitan la ciudad; a partir de la parte 3
dirigen el volante. El título de la ventana muestra siempre la parte, los controles,
la velocidad en km/h, el ambiente, el estado de los faros y el progreso de las entregas.

La partida consiste en tres entregas: hay que llegar a menos de tres unidades del
destino dorado **frenando** (menos de una unidad por segundo) para completarla.

## Diseño de la ciudad

Cada celda mide `10` unidades, igual que en el mapa de 7x7. Con 11 celdas por lado la
ciudad ocupa `110 x 110` unidades y el centro de cada celda está a 55 del origen:

```text
                 X -55 ................................ +55
   Z -50  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  avenida del borde norte
          │ E │ E │ P │ E │ E │          filas de manzana en los índices impares
   Z -30  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
          │ P │ E │ E │ P │ E │
   Z -10  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  avenida central
          │ E │ P │ E │ E │ P │
   Z +10  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  avenida del borde sur
          │ E │ E │ E │ E │ P │          sector nuevo al sur
   Z +30  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
          │ P │ E │ E │ E │ E │          sector nuevo al sureste
   Z +50  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
```

- Las calles ocupan los índices **pares** de filas y columnas: seis avenidas en cada
  eje, totalmente conectadas entre sí.
- Las manzanas ocupan los índices **impares**: `5 x 5 = 25` parcelas, de las que
  **18 llevan edificio y 7 son parques** (el enunciado pide 12 y 4).
- El 7x7 original se conserva **intacto** en la esquina noroeste (filas y columnas
  0 a 6) y todo lo demás son sectores nuevos al este y al sur.
- Las alturas de los edificios varían entre 6 y 22 unidades y los tonos cambian con la
  fila y la columna; los del sureste llevan tres unidades más de altura.
- Cada manzana tiene acera de diez unidades, y los edificios ocupan siete, de modo que
  la acera queda visible alrededor.
- Cada edificio es un cuerpo con el alto y el tono que le tocan, más las ventanas de sus
  cuatro fachadas. No lleva cornisa ni remates: el enunciado solo pide variación de
  altura y color, y ventanas que aparenten encenderse de noche.

## Iluminación

- **Ambiente**: `N` alterna entre día y noche. La luz ambiental diurna es 0,48 y la
  nocturna 0,12/0,16/0,24, con el sol sustituido por una fuente débil y azulada.
- **Farolas**: 10 bombillas en el bordillo de la acera de diez manzanas distintas —las
  esquinas de las manzanas que rodean a los cruces—, a una unidad del límite y mirando a
  la calle; el enunciado pide nueve. El poste arranca en la acera, a 0,3 de altura, y no
  en el suelo del asfalto. El poste y la bombilla se dibujan en las mismas posiciones que
  se envían al shader, así que la luz coincide con el objeto visible. El alcance es de 22
  unidades y el shader descarta antes las bombillas que no llegan, para no pagar diez
  bucles en cada fragmento.
- **Faros del vehículo**: dos focos con cono (`smoothstep` entre 0,85 y 0,97), alcance
  de 26 unidades y atenuación cuadrática. Se encienden y apagan con `F`.
- **Emisión frente a luz recibida**: `uEmision` permite que la bombilla y el halo de los
  faros conserven su color sin ser iluminados. Ese brillo es emisión visual, no el
  mismo fenómeno que la luz que recibe el suelo o las fachadas.
- La luz difusa usa las normales de cada cara (`max(dot(normal, dirección), 0)`), por
  lo que un muro y el suelo reciben cantidades distintas del mismo foco.

## Decoración urbana

- **Parques**: cuatro árboles y un banco en cada una de las siete manzanas de parque.
- **Edificios**: ventanas en las cuatro fachadas, con un piso cada tres unidades y dos
  por fachada. De noche se encienden tres de cada cinco, según una semilla que depende de la
  manzana, la fachada y el piso, para que el barrio no quede plano. El vidrio apagado es el
  45% del tono del cuerpo, no un color fijo, para que la retícula se lea igual en los
  dieciocho edificios.
- **Pasos peatonales**: siete franjas blancas sobre la calle contigua al norte de cada
  manzana.
- **Semáforos**: uno de cada tres manzanas, con ciclo de diez segundos (rojo 0-4,
  verde 4-8, amarillo 8-10) controlado por el reloj de la simulación. Son decorativos:
  cambian de color pero no detienen al vehículo.

## Tráfico autónomo

Tres coches que circulan solos por la ciudad, cada uno con su posición, su rumbo y su
velocidad, y con un color y un ritmo distintos (verde, azul y amarilla; 9, 11 y 8 u/s).

- **Ruta**: se mueven por el centro de las celdas de calle y saltan de intersección en
  intersección, así que por construcción no pueden entrar en una manzana. Además, antes
  de aceptar la posición nueva se consulta `puedeCircular()`, el mismo método que usa el
  vehículo del jugador: si el mapa cambiara, el tráfico se detendría en vez de embarrar.
- **Frenada y giro**: la velocidad objetivo es la curva de frenada clásica
  (`sqrt(2 * deceleracion * distancia)`), de modo que llegan a la esquina a punto de parar,
  giran noventa grados a 3,2 rad/s y vuelven a acelerar. Nunca dan media vuelta, y si ya
  giraron en la esquina anterior prefieren seguir recto para no dar bandos.
- **No se atraviesan entre ellos ni con el jugador**: hay tres reglas que lo garantizan.
  Las avenidas son de **un solo sentido**, alternadas por parejas, así que dos coches nunca
  se encuentran de frente. El que va detrás frena al de delante y **deja además cinco
  unidades de margen**, porque si solo se detuvieran al tocar, el que espera se quedaría
  en medio del cruce y el otro no tendría por dónde pasar. En un cruce, cede el de índice
  mayor, de modo que siempre hay alguien con paso libre y nadie queda atascado. El jugador
  manda sobre todos: el tráfico se aparta y su coche frena contra ellos igual que contra
  un edificio, sin rebote.
- Si el jugador se queda **parado en mitad de una avenida**, los coches se acumulan detrás
  de él y esperan hasta que se mueve. Es lo que hace un tráfico real con un coche parado,
  y `R` despeja la situación.
- Se reinician con `R`, junto con el resto de la partida.

## Minimapa y panel

- El minimapa aparece en la esquina superior derecha y se oculta con `M`. Se dibuja en
  un segundo pase, con `glScissor` para no borrar la vista principal, `glViewport`
  propio y la proyección ortográfica del shader (`uMapa = 1`), con el norte hacia arriba
  y toda la ciudad ampliada dentro del recuadro. El encuadre se recalcula en cada cuadro
  a partir de `ancho` y `alto`, así que se mantiene al redimensionar la ventana.
- Muestra el vehículo como una marca cian orientada con una punta blanca por delante, y
  el destino activo en dorado.
- El panel de información ocupa la esquina superior izquierda. Se dibuja con el tercer
  modo de proyección del shader (`uMapa = 2`), que trabaja en píxeles y por eso no
  necesita cambiar el viewport: solo el recorte y desactivar la profundidad, ya que el
  fondo 3D está escrito en esa zona. Muestra velocidad con su barra, ambiente, faros,
  progreso de las entregas, destino y distancia, y una ayuda de controles.
- La tipografía es una fuente de mapa de bits 3x5 dibujada con cajas: cada punto
  encendido es un cubo del tamaño de un píxel. No se usa ninguna fuente externa.

## Reinicio

`R` devuelve el vehículo a la intersección de partida, con velocidad cero, y vuelve a
activar la primera entrega, de modo que se puede repetir el recorrido completo tantas
veces como haga falta.

## Organización del código

```text
Motor         ventana, ciclo de dibujo, cubo unitario, uniforms, shaders
  └─ Ciudad        mapa 11x11, suelo, aceras, manzanas, marcas viales, cámara aérea
       └─ Conduccion     auto, conducción, colisiones, cámaras, reinicio
            └─ Iluminacion     día y noche, farolas, faros, shader de luz
                 └─ CiudadCompleta  entregas, decoración, semáforos, minimapa, panel
Texto         fuente 3x5 para el panel (utilidad, no es una etapa)
```

Cada etapa es ejecutable por separado, reutiliza la anterior con `super` y anuncia su
número en el título de la ventana.

## Progreso

| Parte | Contenido | Estado | Verificado con |
|---|---|---|---|
| 1 | `Motor`: ventana, ciclo, cubo unitario, uniforms, shaders con tres proyecciones | completada | ventana real, 8 cuadros, sin errores OpenGL |
| 2 | `Ciudad`: mapa 11x11, suelo, aceras, edificios, parques, marcas viales, cámara aérea | completada | 7 pruebas de mapa en verde, 10 cuadros dibujados |
| 3 | `Conduccion`: auto, conducción, colisiones, cámaras, reinicio | completada | 6 pruebas de colisión en verde, 40 cuadros dibujados |
| 4 | `Iluminacion`: día y noche, 16 farolas, faros del vehículo | completada | 5 pruebas de farolas en verde, shader GLSL compilado, 300 cuadros a ~50 fps |
| 5 | `CiudadCompleta`: entregas, decoración, semáforos, minimapa, panel | completada | 6 pruebas de entregas y panel en verde, lectura de píxeles del framebuffer |
| 6 | `Trafico`: tres coches autónomos con ruta, frenada, giro y colisiones | completada | 14 pruebas de tráfico en verde, 38 pruebas en total |

## Pruebas

```sh
mvn test
```

35 pruebas sin ventana, repartidas en cinco clases:

- `CiudadTest`: tamaño del mapa, distribución urbana, conservación del 7x7 original,
  red vial conectada por inundación, manzanas rodeadas de calle, centros y alturas.
- `ConduccionTest`: calles transitables, todas las manzanas bloqueadas, radio del
  vehículo, los cuatro bordes del mapa, reinicio y frente del vehículo.
- `IluminacionTest`: cantidad de farolas, que estén sobre calle, dentro del mapa y
  repartidas por los cuatro cuadrantes, y alcances suficientes.
- `CiudadCompletaTest`: destinos sobre calle y aparcables, reparto de las paradas, ciclo
  del semáforo, reinicio de partida y contenido del panel con caracteres que la fuente
  puede dibujar.
- `TraficoTest`: tres coches que arrancan sobre calle, avanzan con el tiempo, giran en las
  esquinas sin dar media vuelta, siguen siempre una celda de calle y se reinician.
  La más exigente simula 18000 cuadros comprobando en cada posición de cada coche que no
  entra nunca en una manzana, que no se sale del mapa y que ningún coche se solapa con
  otro ni con el jugador.

## Limitaciones

- La iluminación es local: no hay sombras ni oclusión, así que una farola puede
  iluminar a través de un edificio.
- Los semáforos son decorativos; no detienen al vehículo.
- No hay peatones, sonido, modelos importados ni ruedas animadas.
- El choque detiene el auto sin rebote: no hay recuperación automática.
- El cielo mantiene el mismo fondo en día y en noche, para que la comparación se centres
  en la iluminación de las superficies.
- Siniperspectiva calculada en el shader: no se usa ninguna librería de matrices, por lo
  que el campo de visión está fijo en 55 grados.
- En este equipo el arranque muestra un aviso de LWJGL sobre la versión de JNI al
  ejecutarse con el JDK 25; la compilación usa el JDK 21 del *toolchain*. No afecta al
  resultado y desaparece con JDK 17.

## Documentación de apoyo

- `REGLAS.md`: reglas de estilo y arquitectura que sigue el proyecto.