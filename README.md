# Carmesí

El launcher de juegos del móvil. Hermano de [Atrio](https://github.com/gvegagato-prog/Atrio-Suite-2.0): mismo sistema de temas
(las mismas paletas, letras y «estilos»), otro paquete (`com.gonzalo.portico`), así que **conviven en el mismo móvil** y se ven
como dos programas de la misma suite. Atrio es la puerta de la casa; Carmesí es la galería de arcos.

Carmesí **no es un emulador**: abre los que ya tienes instalados, los juegos de Android y los juegos de navegador, todo desde
una sola biblioteca.

## 0.20.0: la ficha sin casillas que mover, los widgets eligen juego

- **La ficha, como la de Marfil**: las pestañas de estado (Jugando · Pendiente · Pasado · Abandonado) van arriba, pegadas a la barra. Debajo, el
  titular, la portada (tocarla la cambia: SteamGridDB, internet, galería, encuadrar) y las horas; luego «Jugar» (o «Vincular») y «Saga», y las
  casillas fijas: Cómo se abre, En Azabache, Lo que suele durar y En la nube. **Fuera el modo de mover casillas** (eso se queda en Azabache):
  «Editar» ahora es el nombre, la plataforma, corregir las horas y quitar el juego. Fuera también «Unir con otro», «Renombrar», «Portada» y
  «Acceso directo» como botones.
- **Los widgets eligen juego**: al poner «Sigue jugando» o «En portada» en el escritorio, Android pregunta qué juego abre: «el que toque» (como
  hasta ahora) o uno fijo. Es lo que hacían los accesos directos, pero con el dibujo de la revista.
- En la portada, los filetes de los estantes son de tinta (con fuerza), como los de Marfil.

## 0.19.0: la ficha como la de Marfil, SteamGridDB, horas de Steam y encuadre

- **La ficha**: la portada grande a la izquierda y, a su lado, tus horas en grande (o, si se sabe lo que dura el juego, el **porcentaje de la
  historia** con su barra, como el porcentaje de un libro en Marfil), de dónde salen las horas (este móvil, el PC, Steam…), veces y última vez.
  Debajo, el estado como pestañas y las acciones. Las casillas de siempre («En Azabache», «Lo que suele durar» —historia, extras, completo, de
  Atrio—, «Cómo se abre») siguen debajo y se siguen pudiendo editar.
- **SteamGridDB**: en Portada → «SteamGridDB» buscas el juego y eliges una de sus portadas (600×900). Y **horas de Steam** (Ajustes → Servicios →
  «Traer las horas de Steam», y solas cada 6 h): se ponen en los juegos que se llamen igual; los del PC no las reciben porque Atrio del PC ya las
  cuenta (nada se suma dos veces). Las claves (API key de SteamGridDB, clave y perfil de Steam) **se cogen solas de las que tienes en Atrio**
  (están en tu cuenta); si no, se ponen en Ajustes → Servicios.
- **Encuadrar la portada**: se guarda la imagen entera que bajes (de SteamGridDB, de internet o de la galería) y en Portada → «Encuadrar» la
  mueves y la amplías dentro del hueco, como se hacía con las imágenes del lienzo. El encuadre vale para la ficha, la portada y los widgets, y
  viaja con tu cuenta.
- **Lo que suele durar** (HowLongToBeat/RAWG, lo que Atrio tenga) viene al importar y sirve para el porcentaje.

## 0.18.0: la portada, como una revista (y con mando)

Fuera el lienzo de casillas con colores: cada casilla traía su propio papel y, sobre el granate (o cualquier fondo), el inicio parecía un mosaico
de aparatos. Ahora la portada es una **portada de revista**, todo sobre el papel de la página, con el único color de las carátulas:

- **Destacado**: el juego de ahora (lo que estés jugando; si el PC está ejecutando algo, eso, en directo; o el que tú fijes), con su arte a
  sangre dentro del marco de la revista, fundido con el papel por abajo, y encima el antetítulo, el titular y «Jugar → · Ficha →».
- **Estantes**: filas de portadas con un marco idéntico (un filete fino) y el nombre y de dónde es debajo, en versalitas. Los que quieras, en el
  orden que quieras, con portadas grandes o pequeñas: Sigue jugando, Mis elegidos (los que tú pongas, en tu orden), Pendiente, Sagas (abanicos),
  Tu PC y Pasado. «Editar» lo configura; viaja con tu cuenta (gana el cambio más reciente).
- **Mando y teclado**: todo lo que se toca se enfoca. La cruceta se mueve por estantes y portadas (lo enfocado lleva un filete de acento y crece un
  poco), **A o Enter juega**, **X o Y abre la ficha**. Con el dedo: un toque juega, mantener abre la ficha. Es el mismo diseño (una lista de
  entradas: estante, tamaño, juegos fijados) que podrá pintar Carmesí en el PC con mando o con ratón.
- Sin colores por casilla: la personalización es qué va, dónde y de qué tamaño.

## 0.17.0: Carmesí hereda el PC de Atrio, y sin valoración

Primera versión que se sube como `Carmesi-<versión>.apk` (la 0.16.0 ya entendía los dos nombres). Icono nuevo.

- **Tu PC**, como en Atrio: en el menú de la portada y, encima del lienzo, la tarjeta **«En directo»** con lo que Atrio del PC está ejecutando
  (cronómetro, captura, streaming, su ficha). La pantalla tiene cuatro pestañas: **Ahora** (lo que se juega, últimas capturas, seguir jugando),
  **Biblioteca** (todos los juegos del PC: jugar en el PC, streaming o añadirlos a Carmesí), **Capturas** (las que pides al PC, a su resolución
  original; opcionalmente también en Imágenes/Carmesí) y **Equipo** (traer Atrio, rebuscar la biblioteca, Steam Link y encender el PC por red,
  suspender/reiniciar/apagar, un mensaje en su pantalla y los dispositivos de tu cuenta). La app del PC no cambia: es el mismo protocolo de Atrio.
- **Juegos del PC** dentro de Carmesí (menú PC): entran solos los que has jugado en el PC (con horas) o que ya tenías de Atrio (la ficha pasa a
  ser el juego del PC, con su clave antigua como alias); los demás, desde «Tu PC → Biblioteca → Añadir a Carmesí». Las horas del PC cuentan como
  de un dispositivo más, sin sumarse dos veces; lo que el PC está ejecutando pasa a «Jugando», y la partida, al acabar, va a las barras del día.
- **Jugar en el PC o en streaming** desde cualquier sitio: la ficha, la lista, el lienzo de la portada (coloca un juego del PC y al tocarlo eliges),
  los widgets y los accesos directos. El streaming va con Steam Link (si falta, te lleva a instalarlo; si el PC está apagado y publicó su MAC,
  te ofrece encenderlo por red).
- **Sin valoración en Carmesí**: la casilla «Valoración» de la ficha pasa a ser **«En Azabache»**, solo lectura (la general de 0 a 100 y la reseña,
  con «Abrir» para ir a esa ficha en Azabache). La nota ya no sale en las listas ni en los widgets, y el orden «Nota» pasa a «Pasado» (por fecha).
  Valorar se hace en Azabache; aquí solo se ve.
- La lista negra de Atrio se respeta: lo que vetaste allí no sale aquí.
- Fuera las guías y los apuntes del PC (como pediste), y las fotos entre dispositivos (eso es de Atrio).

## 0.16.0: Pórtico pasa a ser Carmesí

La primera versión con el nombre nuevo (el paquete sigue siendo `com.gonzalo.portico`, así que se actualiza sola sobre Pórtico; en kDrive
se llama `Portico-0.16.0.apk` para que la reconozca, y desde la siguiente, `Carmesi-<versión>.apk`). Lo que cambia:

- **Secciones nuevas**: Jugando · Pendiente · Pasado · Abandonado · Sagas, como páginas por las que te mueves con el dedo o con las pestañas
  (igual que en Marfil). Dentro de cada una, los menús pequeños **PC · Emulación · Android · Web** dicen de dónde es el juego: un toque filtra, otro
  toque en el activo lo quita. No hay «Todos». «Sistemas y emuladores» está en Ajustes.
- **Todos tus juegos, aunque no se abran desde el móvil**: «Añadir → Solo la ficha» crea un juego de PC o de consola con su plataforma, para llevar
  sus horas, su estado y su nota. Las consolas van en Emulación; PC, en PC. Si luego añades su ROM, su app o su web, **«Vincular»** en la ficha las
  une (y lo que se llame igual se une solo). «Unir con otro» junta dos fichas del mismo juego: se suman horas y veces, se queda el estado más
  avanzado y la nota más alta, y la clave antigua queda como alias (para que Azabache no pierda su valoración).
- **Traer mis juegos de Atrio** (Ajustes → Cuenta de Atrio): solo lee tu cuenta. Trae cada juego con su estado, horas, cuándo lo pasaste, la nota
  (la general de Atrio, de 0 a 100, en medios puntos de 0 a 10), plataforma, favorito, la portada (de internet o la foto tuya de tu kDrive) y las
  **sagas de juegos**. Lo que ya tengas con el mismo nombre no se duplica: se completa.
- **Sagas de juegos**: se crean en Carmesí (desde la ficha, «Saga», o en la sección Sagas), con sus juegos en orden, el que toca ahora, y viajan
  con tu cuenta como el resto.
- **Barras diarias** en la portada, entre la fecha y el menú, como en Atrio: lo jugado hoy, los días seguidos y la semana.
- **Estados** Jugando, Pendiente, Pasado y Abandonado (antes solo había «terminado»). Pasado lleva la fecha.
- **Fuera** los apuntes rápidos y el texto libre de las casillas de la ficha: no tenían sentido.
- Para Azabache, un nodo ligero `tiempo` con lo jugado por día y dispositivo, y un contrato de datos escrito: `docs/CONTRATO-DATOS.md`.
- Las portadas de letra en las casillas pequeñas del lienzo ya no se pisan con el texto.

## Capturas

La interfaz es la «Revista» de Atrio. Generadas en el ordenador con datos de ejemplo (`CapturasTest`): [portada](docs/capturas/2_portada.png) ·
[portada en oscuro](docs/capturas/7_portada_oscuro.png) · [Jugando](docs/capturas/3_juegos.png) · [Pendiente](docs/capturas/3b_pendiente.png) ·
[Pasado](docs/capturas/3c_pasado.png) · [Pasado, solo Emulación](docs/capturas/3d_pasado_emulacion.png) · [Sagas](docs/capturas/3e_sagas.png) ·
[saga abierta](docs/capturas/3f_saga_abierta.png) · [página de una saga](docs/capturas/3g_pagina_saga.png) · [ficha](docs/capturas/12_ficha_casillas.png) ·
[ficha sin lanzador](docs/capturas/6b_ficha_sin_lanzador.png) ·
[sistemas](docs/capturas/4_sistemas.png) · [ajustes](docs/capturas/5_ajustes.png) · [añadir](docs/capturas/9_anadir.png) ·
[portada con el PC en directo](docs/capturas/22_portada_pc.png) · [portada](docs/capturas/20_inicio.png) · [portada editando](docs/capturas/21_inicio_editando.png) · [Tu PC](docs/capturas/23_tu_pc.png) · [biblioteca del PC](docs/capturas/24_tu_pc_biblioteca.png) ·
[equipo](docs/capturas/25_tu_pc_equipo.png) · [ficha de un juego del PC](docs/capturas/26_ficha_pc.png)

## La portada

Como la portada de Atrio, con **las mismas medidas** (márgenes de 28 dp, cabecera de 92 sp o 132 sp en tableta, filetes, línea del número y la
fecha en 12 sp y el menú de secciones en versalitas de 12 sp entre dos filetes): cabecera de revista, las barras de lo jugado cada día, el **menú**
(Jugando, Pendiente, Pasado, Abandonado, Sagas, Tu PC), el PC en directo si lo hay y, debajo, el **destacado** y los **estantes** de portadas
(ver «0.18.0» arriba). Capturas: [portada](docs/capturas/20_inicio.png) · [estantes](docs/capturas/20b_inicio_estantes.png) ·
[editando](docs/capturas/21_inicio_editando.png).

## (Antes, 0.15–0.17) La pantalla de inicio era un lienzo

Una cuadrícula de 4 columnas de casillas invisibles donde colocabas juegos, imágenes y widgets con el tamaño y el color que quisieras,
sincronizada entre móviles. Se quitó en la 0.18.0 porque cada casilla traía su propio papel y el conjunto no casaba con la revista.

## (Antes) La pantalla de inicio era un launcher de baldosas

Cabecera de revista compacta y, debajo, **baldosas** con la misma disposición que los widgets (portada a la izquierda, nombre a la derecha, el papel de la revista y
su reborde fino de tinta). **Un toque juega; mantener pulsado abre la ficha.** Pestañas: Recientes, Favoritos, ROMs (con filtro por sistema), Android, Web y Todo;
«Buscar» filtra en el sitio entre todos tus juegos, y la última baldosa («Añadir») es el atajo para meter más. «Lista» abre la lista de siempre, con orden y filtros;
«Sistemas» y el engranaje, sus pantallas.

## Qué hace

- **ROMs con emuladores externos** (GBA, DS, SNES, N64, PSP, PS1/PS2, Dreamcast, 3DS… hasta 170 sistemas). Eliges la ROM o una
  carpeta entera; Carmesí reconoce el sistema por la extensión o por el nombre de la carpeta, y la abre con el emulador que
  elijas por sistema o por juego (melonDS, DraStic, mGBA/RetroArch, Pizza Boy, My Boy!, SkyEmu…). Sin elegir nada, usa uno
  propio instalado y, si no, RetroArch.
- **Juegos de Android**: elige de las apps instaladas (marca primero las que Android declara como juegos).
- **Juegos de navegador** (PokéRogue y similares): pantalla completa, sin barra de direcciones, con cookies y datos guardados
  entre sesiones (ahí viven las partidas), pantalla siempre encendida, orientación fija si quieres, y confirmación antes de
  salir. Deslizar hacia abajo no recarga ni rebota (overscroll desactivado). Funciona con mando Bluetooth (la API Gamepad de Chromium).
- **Accesos directos** en el escritorio del móvil para cualquier juego.
- **Pantalla de inicio**: opcionalmente puedes elegir Carmesí como launcher del móvil (Estilo → Pantalla de inicio).
- **Portadas desde internet** (como en Atrio): en la ficha, Portada → «Buscar en internet» abre Google Imágenes, Pinterest, ArtStation o Newgrounds dentro de Carmesí, con su casilla de búsqueda; tocas una imagen (o la mantienes pulsada) y se usa de portada. También puedes elegir una de la galería.
- **Ficha con casillas editables** (el sistema de maquetas de Atrio, con menos casillas): filas de una o dos casillas separadas por una línea
  que arrastras (⇔) y con alto que arrastras (⇕); subir, bajar, dividir, unir, quitar y añadir filas. Casillas: horas jugadas (editables),
  veces y última vez, estado (jugando, pendiente, pasado, abandonado), valoración (solo cuando está pasado; 0–10 en pasos de 0,5), cómo se abre e
  imagen tuya (galería o búsqueda en internet). Cada juego guarda su diseño y puedes usar el de uno para todos los de su tipo.
- **Fichas sin lanzador** (PC, consola…), sagas de juegos, importar de Atrio y unir fichas: ver «0.16.0» arriba.
- Tiempo de juego aproximado, favoritos, búsqueda, orden, portadas propias, portadas automáticas (libretro), copia de seguridad.

## Diseño: la misma interfaz que Atrio

Carmesí usa la interfaz «Revista» de Atrio, con sus mismas piezas (copiadas de `Revista.kt`, `RevistaUI.kt`, `Ediciones.kt` y `Maqueta.kt`):
papel crema (o oscuro), Fraunces para los titulares, filetes gruesos y finos, antetítulos con cuadradito de acento, pestañas subrayadas,
enlaces con flecha, cifras enormes y puntos guía. No hay dock: se navega desde el menú de la portada y con «← Carmesí».
Ajustes → **Apariencia** es la de Atrio: Tema (claro, oscuro, como el móvil), **Fondo** de color, Tamaño del texto, **Acento**, y «A tu manera» (color del fondo, del texto y del acento a mano, y la **letra** de los títulos y del texto). En «A tu manera», cada color (fondo, texto y acento) tiene su enlace **«Copiar de Atrio»**, que copia solo ese color (el que Atrio tiene a mano o, si no, el que se ve en Atrio según su tema, fondo y acento). El código de un color acepta #RRGGBB, RRGGBB, #RGB y #AARRGGBB, en mayúsculas o minúsculas, escrito o pegado, y se aplica en cuanto es válido. Además, «Colores de Atrio, en directo» (sigue los de la app instalada, con opción de invertirlos) y **«Copiar de Atrio»**: Atrio sube su apariencia a tu cuenta; Carmesí la lee (tema, fondo, acento, colores a mano, letras, tamaño) y copia solo lo que marques, sin tocar nada en Atrio (hace falta la cuenta vinculada, no Atrio instalado en este móvil). La apariencia de Carmesí (tema, fondo, acento, colores a mano, letras, tamaño) también **viaja con tu cuenta** entre tus Carmesís (gana el cambio más reciente) y repinta los widgets de fuera. Probado contra el Firebase de mentira de Atrio, no con tu cuenta real.
**Colores de Atrio** (Ajustes → Apariencia → Colores de): Carmesí lee los colores que tienes puestos en Atrio (paleta, fondo de color, colores a mano, acento, claro u oscuro) y los usa; con **Invertir** los pone del revés: el fondo con el color del texto y el texto con el del fondo, y el acento se mantiene.
Para eso Atrio 1.0.7 (o más nueva) ofrece sus colores en `content://com.gonzalo.gamerate.tema/colores` (solo colores, ningún dato tuyo). Sin esa versión, o sin Atrio, Carmesí usa su propia paleta y te lo dice en Ajustes.

## Cuenta de Atrio y sincronización entre tus Carmesís

Ajustes → **Cuenta de Atrio**: entras con el mismo usuario y contraseña que en Atrio (Firebase Authentication por REST; la contraseña no se guarda, solo el pase de acceso, que se renueva solo).
Carmesí no crea cuentas. Lo que se sincroniza, **solo entre tus Carmesís** (no con Atrio), igual que Atrio entre sus dispositivos (cada dispositivo escribe en su hueco bajo `/users/<tu uid>/portico/`, con sellos de hora y escucha en directo):
juegos (por clave: sistema + nombre de archivo, paquete, web o, en las fichas sin lanzador, el título), favorito, estado, nota, plataforma, alias, nombre y diseño de la ficha, y las sagas. Gana el cambio más reciente; lo que quitas viaja como «borrado»;
las **horas y las veces se suman** (cada dispositivo sube las suyas). Una ROM que viene de otro móvil aparece «en otro dispositivo» hasta que la añades aquí (se une sola por sistema y nombre de archivo).
Tu pareja de Atrio puede leer esa parte de la cuenta (las reglas de Atrio dejan leer a la pareja todo `/users/<uid>`).
Las ROMs, los guardados y las portadas van por kDrive (siguiente sección). Probado en el ordenador contra el Firebase de mentira de Atrio (`SincroTest`) y las reglas de mezcla (`MezclaTest`); no con tu cuenta real.

## Actualizaciones automáticas (kDrive, como Atrio)

Carmesí se actualiza sola con el mismo sistema que Atrio: cada versión se sube a **«Mi carpeta/versiones»** de tu kDrive como `Portico-<versión>.apk` (junto a las `Atrio-…apk`; cada app solo mira las suyas).
Se mira al abrir, cada minuto mientras se ve y, con la app cerrada, cada 4 horas (WorkManager). Si hay una más nueva se baja, se comprueba que es Carmesí y que lleva la misma firma, y se instala
**sin preguntar** cuando no la usas (Android 12+, con el permiso de instalar apps); si Android no lo deja, te pregunta, y al terminar se vuelve a abrir (con «Mostrar sobre otras apps») o te avisa.
Ajustes → **Actualizaciones**: interruptor, token y número de tu kDrive (el token de Atrio es solo de Atrio y no se comparte: hay que pegarlo aquí una vez; se guarda solo en el móvil), «Buscar ahora» y el estado de la última comprobación.
La primera vez que se instala una versión con esto hay que hacerlo a mano; a partir de ahí, sola. Probado contra el kDrive de mentira de Atrio (`ActualizadorTest`), no contra tu kDrive ni instalando de verdad en un móvil.

## Guardados, ROMs y portadas en tu kDrive

Con la cuenta de Atrio vinculada y el kDrive conectado (Ajustes), en la ficha de una ROM aparece **En la nube**:
- **Guardados**: eliges la carpeta donde el emulador deja los `.srm`/`.sav`/`.dsv`/… de ese sistema (la misma para todos sus juegos; se busca por el nombre del archivo de la ROM, también en subcarpetas).
  **Antes de jugar**, Carmesí mira si en la nube hay un guardado distinto del tuyo y que no sea el que ya habías sincronizado; si lo hay, lo baja (comprobando que llega entero) y lo pone en su sitio, dejando antes una copia de lo que sustituye (`copias_guardados`, las 5 últimas).
  **Al volver a Carmesí**, si el guardado de este móvil ha cambiado, lo sube. Si los dos móviles cambiaron el mismo guardado sin sincronizar entre medias, gana el de la nube y la copia te protege.
  No hay vigilancia mientras juegas (Android no avisa de cuándo un emulador escribe un archivo): se mira al lanzar y al volver.
- **ROMs**: se **suben solas** a tu kDrive (a trozos, como en Atrio) las que aún no están, solo con wifi (WorkManager; si se corta, reintenta la ROM entera). Interruptor en Ajustes → Actualizaciones. En el otro móvil, «Descargar la ROM» a la carpeta que elijas (hace falta elegir carpeta); se une sola al juego. Solo a tu kDrive.
- **Portadas** que elegiste: se suben solas y las bajan tus otros móviles si no tienen portada (si ya tienen otra, no se pisa).
- El índice (hash, id de kDrive y nombre de archivo, nunca rutas de tu móvil) viaja en tu cuenta: `/users/<uid>/portico/nube/…`; los archivos, en «Mi carpeta/Portico/…».

**Límite importante**: Android no deja a una app leer las carpetas privadas de otra (`Android/data/<emulador>/…`; el permiso «todos los archivos» tampoco las incluye, según la FAQ oficial de Android 11). Muchos emuladores guardan ahí por defecto (según guías de terceros, Pizza Boy y My Boy!).
Carmesí solo puede sincronizar guardados que estén en una carpeta que Android te deje elegir: configura el emulador para guardar en una carpeta normal del almacenamiento (RetroArch permite cambiar la carpeta de «Savefile» en Ajustes → Directorio). Probado con carpetas normales y los servidores de mentira de Atrio (`NubePTest`); **no con SAF real ni con ningún emulador en un móvil**.

## Widgets

Seis widgets, copiados de los de Atrio (mismo lienzo: papel, titulares en Fraunces, portadas recortadas, esquinas redondas y el reborde fino de tinta)
pero con tus juegos: **Sigue jugando**, **Recientes** (la estantería; tocar un juego lo lanza), **En portada**, **¿Y ahora qué?** (una propuesta distinta cada día) la **marca** «CARMESÍ.» sobre tu fondo y **CARMESÍ en papel** (con su papel, el número del día y la fecha). Usan los colores del tema que tengas puesto (los de Carmesí o los de Atrio, invertidos o no) y se repintan cuando cambias el tema, los
colores de Atrio o la biblioteca. Con los colores de Atrio no dependen de claro/oscuro; con los de Carmesí, «como el sistema» lleva los dos dibujos y se ve el que toca.
Las letras también son las del tema (Ajustes → Estilo): las que vienen con la app, Vitrina o las de Google Fonts (se piden a los servicios de Google, igual que en la app; si no llegan, se usa la letra de la app).
Vistas previas en `docs/widgets/`.

## Icono

El marco de Atrio (la puerta) en versión baja y cuadrada, con la luz roja por dentro en vez de arena, sobre el mismo azul noche con su difuminado interior.

## Cómo funciona el lanzamiento (y qué no está probado)

Los comandos para abrir cada emulador **no están escritos a mano**: salen de los archivos de ES-DE para Android
(`es_systems.xml` y `es_find_rules.xml`, licencia MIT; ver `NOTICE`) y `Comando.kt` los interpreta (acción, datos, extras,
banderas, permisos de lectura al documento). Eso se prueba en el ordenador con el catálogo real (`ComandoTest`).

**Lo que NO se ha podido probar**: abrir de verdad cada emulador en un móvil. Esta versión se escribió y se verificó sin
dispositivo. Si un emulador no abre una ROM, el primer sospechoso es el permiso de acceso al archivo: algunos emuladores
necesitan que les des acceso a la carpeta dentro de sus propios ajustes (ES-DE documenta lo mismo), y RetroArch necesita su
núcleo descargado. Carmesí te lo dice en la ficha del juego («Se abre con…»).

## Compilar

```
gradle :app:assembleDebug        # o ./gradlew
gradle :app:testDebugUnitTest --tests '*ComandoTest*'
gradle :app:testDebugUnitTest --tests '*CapturasTest*' -Proborazzi.test.record=true   # capturas en app/build/outputs/roborazzi
```

Necesita JDK 17+ y el SDK de Android (`local.properties` con `sdk.dir`). El APK de release se firma con `app/portico.keystore`
(misma costumbre que Atrio: clave fija para que cada versión se instale encima de la anterior).

## Privacidad

- Portadas automáticas: pide `thumbnails.libretro.com/<sistema>/Named_Boxarts/<nombre del archivo>.png`, es decir, **el nombre
  de tus archivos** llega a ese servidor. Se apaga en Estilo → «Portadas automáticas».
- Los juegos web guardan cookies (también de terceros: muchos inician sesión en otro dominio) dentro del WebView de Carmesí.
- Si vinculas tu cuenta de Atrio, tu biblioteca (títulos, notas, horas, favoritos…) se sube a tu base de datos de Firebase, legible por ti y por tu pareja de Atrio. Sin vincular, nada más sale del móvil.
