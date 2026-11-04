# Contrato de datos de Carmesí (lo que otras apps pueden leer)

Todo vive en tu cuenta de Firebase bajo `/users/<uid>/portico/` (el nodo se llama así desde los tiempos de Pórtico y no se cambia para no romper
la sincronización). Carmesí escribe; Azabache (y quien venga) **lee**. Nada de esto cambia Atrio.
Es el hermano del contrato de Marfil (`lectura-app/docs/CONTRATO-DATOS.md`): mismos nombres y mismas ideas.

## Identidad de un juego

La `clave` de un juego es lo que lo une entre dispositivos y con su valoración en Azabache:

| Tipo | Clave | Ejemplo |
|---|---|---|
| ROM | `rom:<sistema>:<nombre del archivo normalizado>` | `rom:gba:pokemonesmeralda` |
| App de Android | `app:<paquete>` | `app:com.chucklefish.stardewvalley` |
| Web | `web:<dirección sin esquema ni www>` | `web:pokerogue.net` |
| Ficha sin lanzador (PC, consola…) | `juego:<título normalizado>`, **fijada al crear la ficha** (no cambia al renombrar) | `juego:hades` |

`norm` = minúsculas, sin tildes y solo letras y números. Un juego importado de Atrio nace como ficha con la clave `juego:<título>`.
Cuando a esa ficha se le pone su ROM, su app o su web (o se unen dos fichas), el juego se queda con la clave del lanzador y la antigua pasa a
**`alias`**. Azabache debe buscar por la clave **y por los alias**: la valoración que hizo con `juego:hades` sigue siendo la de `rom:switch:hades`.

En la cuenta un juego se llama `kid` = los 16 primeros hex del SHA-1 de su clave (la clave tiene puntos y barras, que Firebase no admite).

## Nodos

| Nodo | Contenido | Quién escribe |
|---|---|---|
| `dev/<dispositivo>/data` | La biblioteca de ese dispositivo (texto JSON `v:1`): por juego `k` (clave), `t` título, `tp` tipo (`ROM`/`APP`/`WEB`/`FICHA`), `s` sistema, `pk` paquete, `u` web, `a` archivo, `pl` plataforma, `al` alias, `f` favorito, `es` estado (`JUGANDO`/`PENDIENTE`/`PASADO`/`ABANDONADO`), `te` terminado, `fi` cuándo lo pasó (ms), `n` nota (0–10 en medios puntos), `m` diseño de la ficha, `an` añadido, `ul` última vez, `mod`, `msp` ms jugados en ese dispositivo, `vp` veces, `dd` ms por día (`"AAAA-MM-DD"` → ms); `borrados` (clave → hora); `sg` sagas de juegos (`id`, `n` nombre, `k` claves en orden, `mod`) y `sgb` sagas borradas; `pe`/`pb` la pantalla de inicio; `aj` la apariencia. | cada Carmesí, solo el suyo |
| `stamps/<dispositivo>` | Hora de la última subida de ese dispositivo (servidor). | cada Carmesí |
| `tiempo/<kid>/<dispositivo>` | `{t, k, al, m, p, total, days, fin}`: título, clave, alias, menú (`pc`/`emulacion`/`android`/`web`), plataforma, ms jugados en ese dispositivo, `days` (`"AAAA-MM-DD"` → ms) y cuándo lo pasó. **Ligero**: es lo que debe leer un Diario. Cada dispositivo escribe solo lo suyo y borra lo de los juegos que ya no tiene. | cada Carmesí |
| `nube/portadas/<kid>` | La portada que pusiste tú, en kDrive (`f.img`: id, nombre, hash, tamaño). | cualquier Carmesí |
| `nube/roms/<kid>`, `nube/guardados/<kid>` | La ROM y las partidas guardadas en kDrive. | cualquier Carmesí |

## Cómo leer el tiempo para un Diario

Para cada juego, lee `tiempo/<kid>/*` y **suma** los dispositivos (cada uno trae solo lo suyo; no hay nada repetido). Con eso salen los
minutos de cada día y por juego. Las horas que vinieron de Atrio al importar cuentan como del dispositivo que importó.

## La nota, en los dos sitios

La nota de Carmesí (0–10, medios puntos) y la general de Azabache (0–100) son la misma: `general = nota × 10`. Se edita en los dos y
prevalece el cambio más reciente (`mod` del juego en Carmesí frente a `updated` en Azabache).

## Lo que aún no está (se añadirá cuando Azabache lo pida)

- Los juegos de PC con lanzador (`pc:<id>`) y el streaming: vendrán con la parte de «jugar en el PC».
- Las fotos y capturas del PC.
