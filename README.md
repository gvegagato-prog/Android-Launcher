# Pórtico

El launcher de juegos del móvil. Hermano de [Atrio](https://github.com/gvegagato-prog/Atrio-Suite-2.0): mismo sistema de temas
(las mismas paletas, letras y «estilos»), otro paquete (`com.gonzalo.portico`), así que **conviven en el mismo móvil** y se ven
como dos programas de la misma suite. Atrio es la puerta de la casa; Pórtico es la galería de arcos.

Pórtico **no es un emulador**: abre los que ya tienes instalados, los juegos de Android y los juegos de navegador, todo desde
una sola biblioteca.

## Capturas

Generadas en el ordenador con datos de ejemplo (`CapturasTest`): [inicio](docs/capturas/2_inicio.png) ·
[juegos](docs/capturas/3_juegos.png) · [ficha](docs/capturas/6_ficha.png) · [sistemas](docs/capturas/4_sistemas.png) ·
[estilo](docs/capturas/5_estilo.png) · [estilo Atrio en claro](docs/capturas/7_inicio_clasico_claro.png) ·
[estilo Arcade](docs/capturas/8_juegos_arcade.png)

## Qué hace

- **ROMs con emuladores externos** (GBA, DS, SNES, N64, PSP, PS1/PS2, Dreamcast, 3DS… hasta 170 sistemas). Eliges la ROM o una
  carpeta entera; Pórtico reconoce el sistema por la extensión o por el nombre de la carpeta, y la abre con el emulador que
  elijas por sistema o por juego (melonDS, DraStic, mGBA/RetroArch, Pizza Boy, My Boy!, SkyEmu…). Sin elegir nada, usa uno
  propio instalado y, si no, RetroArch.
- **Juegos de Android**: elige de las apps instaladas (marca primero las que Android declara como juegos).
- **Juegos de navegador** (PokéRogue y similares): pantalla completa, sin barra de direcciones, con cookies y datos guardados
  entre sesiones (ahí viven las partidas), pantalla siempre encendida, orientación fija si quieres, y confirmación antes de
  salir. Funciona con mando Bluetooth (la API Gamepad de Chromium).
- **Accesos directos** en el escritorio del móvil para cualquier juego.
- **Pantalla de inicio**: opcionalmente puedes elegir Pórtico como launcher del móvil (Estilo → Pantalla de inicio).
- Tiempo de juego aproximado, favoritos, búsqueda, orden, portadas propias, portadas automáticas (libretro), copia de seguridad.

## Diseño: el guiño a Atrio

- Mismas paletas (todas las de Atrio, claras y oscuras), mismas combinaciones de letra y color («Vitrina», «Arcade», «Terminal»…),
  mismo dock en píldora flotante, mismos chips de tinta llena.
- Lo propio: **la portada es un arco** (la puerta del icono de Atrio, repetida), el dock lleva el «+» en medio, y la forma del
  arco cambia con el estilo (redondo, biselado o recto con borde y sombra dura según el estilo elegido).
- El icono son tres arcos seguidos, con la luz de Atrio en el del centro.

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
- Nada más sale del móvil. No hay cuenta ni sincronización.
