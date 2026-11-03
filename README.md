# Pórtico

El launcher de juegos del móvil. Hermano de [Atrio](https://github.com/gvegagato-prog/Atrio-Suite-2.0): mismo sistema de temas
(las mismas paletas, letras y «estilos»), otro paquete (`com.gonzalo.portico`), así que **conviven en el mismo móvil** y se ven
como dos programas de la misma suite. Atrio es la puerta de la casa; Pórtico es la galería de arcos.

Pórtico **no es un emulador**: abre los que ya tienes instalados, los juegos de Android y los juegos de navegador, todo desde
una sola biblioteca.

## Capturas

La interfaz es la «Revista» de Atrio. Generadas en el ordenador con datos de ejemplo (`CapturasTest`): [portada](docs/capturas/2_portada.png) ·
[portada en oscuro](docs/capturas/7_portada_oscuro.png) · [juegos](docs/capturas/3_juegos.png) · [ficha](docs/capturas/12_ficha_casillas.png) ·
[ficha editando casillas](docs/capturas/13_ficha_editando.png) · [sistemas](docs/capturas/4_sistemas.png) · [ajustes](docs/capturas/5_ajustes.png) ·
[añadir](docs/capturas/9_anadir.png)

## La pantalla de inicio es un launcher

Cabecera de revista compacta y, debajo, **baldosas** con la misma disposición que los widgets (portada a la izquierda, nombre a la derecha, el papel de la revista y
su reborde fino de tinta). **Un toque juega; mantener pulsado abre la ficha.** Pestañas: Recientes, Favoritos, ROMs (con filtro por sistema), Android, Web y Todo;
«Buscar» filtra en el sitio entre todos tus juegos, y la última baldosa («Añadir») es el atajo para meter más. «Lista» abre la lista de siempre, con orden y filtros;
«Sistemas» y el engranaje, sus pantallas.

## Qué hace

- **ROMs con emuladores externos** (GBA, DS, SNES, N64, PSP, PS1/PS2, Dreamcast, 3DS… hasta 170 sistemas). Eliges la ROM o una
  carpeta entera; Pórtico reconoce el sistema por la extensión o por el nombre de la carpeta, y la abre con el emulador que
  elijas por sistema o por juego (melonDS, DraStic, mGBA/RetroArch, Pizza Boy, My Boy!, SkyEmu…). Sin elegir nada, usa uno
  propio instalado y, si no, RetroArch.
- **Juegos de Android**: elige de las apps instaladas (marca primero las que Android declara como juegos).
- **Juegos de navegador** (PokéRogue y similares): pantalla completa, sin barra de direcciones, con cookies y datos guardados
  entre sesiones (ahí viven las partidas), pantalla siempre encendida, orientación fija si quieres, y confirmación antes de
  salir. Deslizar hacia abajo no recarga ni rebota (overscroll desactivado). Funciona con mando Bluetooth (la API Gamepad de Chromium).
- **Accesos directos** en el escritorio del móvil para cualquier juego.
- **Pantalla de inicio**: opcionalmente puedes elegir Pórtico como launcher del móvil (Estilo → Pantalla de inicio).
- **Portadas desde internet** (como en Atrio): en la ficha, Portada → «Buscar en internet» abre Google Imágenes, Pinterest, ArtStation o Newgrounds dentro de Pórtico, con su casilla de búsqueda; tocas una imagen (o la mantienes pulsada) y se usa de portada. También puedes elegir una de la galería.
- **Ficha con casillas editables** (el sistema de maquetas de Atrio, con menos casillas): filas de una o dos casillas separadas por una línea
  que arrastras (⇔) y con alto que arrastras (⇕); subir, bajar, dividir, unir, quitar y añadir filas. Casillas: horas jugadas (editables),
  veces y última vez, estado (terminado), valoración (solo cuando está terminado; 0–10 en pasos de 0,5), apuntes, cómo se abre, texto libre e
  imagen tuya (galería o búsqueda en internet). Cada juego guarda su diseño y puedes usar el de uno para todos los de su tipo.
- Tiempo de juego aproximado, favoritos, búsqueda, orden, portadas propias, portadas automáticas (libretro), copia de seguridad.

## Diseño: la misma interfaz que Atrio

Pórtico usa la interfaz «Revista» de Atrio, con sus mismas piezas (copiadas de `Revista.kt`, `RevistaUI.kt`, `Ediciones.kt` y `Maqueta.kt`):
papel crema (o oscuro), Fraunces para los titulares, filetes gruesos y finos, antetítulos con cuadradito de acento, pestañas subrayadas,
enlaces con flecha, cifras enormes y puntos guía. No hay dock: se navega desde el menú de la portada y con «← Pórtico».
Los temas, paletas, acentos y estilos son los mismos que los de Atrio (Ajustes → Apariencia).
**Colores de Atrio** (Ajustes → Apariencia → Colores de): Pórtico lee los colores que tienes puestos en Atrio (paleta, fondo de color, colores a mano, acento, claro u oscuro) y los usa; con **Invertir** los pone del revés: el fondo con el color del texto y el texto con el del fondo, y el acento se mantiene.
Para eso Atrio 1.0.7 (o más nueva) ofrece sus colores en `content://com.gonzalo.gamerate.tema/colores` (solo colores, ningún dato tuyo). Sin esa versión, o sin Atrio, Pórtico usa su propia paleta y te lo dice en Ajustes.

## Cuenta de Atrio y sincronización entre tus Pórticos

Ajustes → **Cuenta de Atrio**: entras con el mismo usuario y contraseña que en Atrio (Firebase Authentication por REST; la contraseña no se guarda, solo el pase de acceso, que se renueva solo).
Pórtico no crea cuentas. Lo que se sincroniza, **solo entre tus Pórticos** (no con Atrio), igual que Atrio entre sus dispositivos (cada dispositivo escribe en su hueco bajo `/users/<tu uid>/portico/`, con sellos de hora y escucha en directo):
juegos (por clave: sistema + nombre de archivo, paquete o web), favorito, terminado, nota, apuntes, nombre y diseño de la ficha. Gana el cambio más reciente; lo que quitas viaja como «borrado»;
las **horas y las veces se suman** (cada dispositivo sube las suyas). Una ROM que viene de otro móvil aparece «en otro dispositivo» hasta que la añades aquí (se une sola por sistema y nombre de archivo).
Tu pareja de Atrio puede leer esa parte de la cuenta (las reglas de Atrio dejan leer a la pareja todo `/users/<uid>`).
**Todavía no se sincronizan** las ROMs, los guardados ni las imágenes (siguiente fase). Probado en el ordenador contra el Firebase de mentira de Atrio (`SincroTest`) y las reglas de mezcla (`MezclaTest`); no con tu cuenta real.

## Actualizaciones automáticas (kDrive, como Atrio)

Pórtico se actualiza sola con el mismo sistema que Atrio: cada versión se sube a **«Mi carpeta/versiones»** de tu kDrive como `Portico-<versión>.apk` (junto a las `Atrio-…apk`; cada app solo mira las suyas).
Se mira al abrir, cada minuto mientras se ve y, con la app cerrada, cada 4 horas (WorkManager). Si hay una más nueva se baja, se comprueba que es Pórtico y que lleva la misma firma, y se instala
**sin preguntar** cuando no la usas (Android 12+, con el permiso de instalar apps); si Android no lo deja, te pregunta, y al terminar se vuelve a abrir (con «Mostrar sobre otras apps») o te avisa.
Ajustes → **Actualizaciones**: interruptor, token y número de tu kDrive (el token de Atrio es solo de Atrio y no se comparte: hay que pegarlo aquí una vez; se guarda solo en el móvil), «Buscar ahora» y el estado de la última comprobación.
La primera vez que se instala una versión con esto hay que hacerlo a mano; a partir de ahí, sola. Probado contra el kDrive de mentira de Atrio (`ActualizadorTest`), no contra tu kDrive ni instalando de verdad en un móvil.

## Widgets

Seis widgets, copiados de los de Atrio (mismo lienzo: papel, titulares en Fraunces, portadas recortadas, esquinas redondas y el reborde fino de tinta)
pero con tus juegos: **Sigue jugando**, **Recientes** (la estantería; tocar un juego lo lanza), **En portada**, **¿Y ahora qué?** (una propuesta distinta cada día) la **marca** «PÓRTICO.» sobre tu fondo y **PÓRTICO en papel** (con su papel, el número del día y la fecha). Usan los colores del tema que tengas puesto (los de Pórtico o los de Atrio, invertidos o no) y se repintan cuando cambias el tema, los
colores de Atrio o la biblioteca. Con los colores de Atrio no dependen de claro/oscuro; con los de Pórtico, «como el sistema» lleva los dos dibujos y se ve el que toca.
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
núcleo descargado. Pórtico te lo dice en la ficha del juego («Se abre con…»).

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
- Los juegos web guardan cookies (también de terceros: muchos inician sesión en otro dominio) dentro del WebView de Pórtico.
- Si vinculas tu cuenta de Atrio, tu biblioteca (títulos, notas, horas, favoritos…, no las ROMs) se sube a tu base de datos de Firebase, legible por ti y por tu pareja de Atrio. Sin vincular, nada más sale del móvil.
