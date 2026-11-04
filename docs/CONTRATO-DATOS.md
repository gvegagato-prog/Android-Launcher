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
| Juego del PC (de la biblioteca que Atrio del PC publica en `suite/lib`) | `pc:<clave en la biblioteca del PC>` | `pc:steam:1145360` |
| Ficha sin lanzador (consola…) | `juego:<título normalizado>`, **fijada al crear la ficha** (no cambia al renombrar) | `juego:hades` |

`norm` = minúsculas, sin tildes y solo letras y números. Un juego importado de Atrio nace como ficha con la clave `juego:<título>`.
Cuando a esa ficha se le pone su ROM, su app o su web (o se unen dos fichas), el juego se queda con la clave del lanzador y la antigua pasa a
**`alias`**. Azabache debe buscar por la clave **y por los alias**: la valoración que hizo con `juego:hades` sigue siendo la de `rom:switch:hades`.

En la cuenta un juego se llama `kid` = los 16 primeros hex del SHA-1 de su clave (la clave tiene puntos y barras, que Firebase no admite).

## Nodos

| Nodo | Contenido | Quién escribe |
|---|---|---|
| `dev/<dispositivo>/data` | La biblioteca de ese dispositivo (texto JSON `v:1`): por juego `k` (clave), `t` título, `tp` tipo (`ROM`/`APP`/`WEB`/`PC`/`FICHA`), `pck`/`pcd` clave y PC del juego del PC, `pu` portada de internet, `s` sistema, `pk` paquete, `u` web, `a` archivo, `pl` plataforma, `al` alias, `f` favorito, `es` estado (`JUGANDO`/`PENDIENTE`/`PASADO`/`ABANDONADO`), `te` terminado, `fi` cuándo lo pasó (ms), `n` nota (ya no se edita en Carmesí; queda por compatibilidad), `m` diseño de la ficha, `an` añadido, `ul` última vez, `mod`, `msp` ms jugados en ese dispositivo, `vp` veces, `dd` ms por día (`"AAAA-MM-DD"` → ms); `borrados` (clave → hora); `sg` sagas de juegos (`id`, `n` nombre, `k` claves en orden, `mod`) y `sgb` sagas borradas; `pe`/`pb` la pantalla de inicio; `aj` la apariencia. | cada Carmesí, solo el suyo |
| `stamps/<dispositivo>` | Hora de la última subida de ese dispositivo (servidor). | cada Carmesí |
| `tiempo/<kid>/<dispositivo>` | `{t, k, al, m, p, total, days, fin}`: título, clave, alias, menú (`pc`/`emulacion`/`android`/`web`), plataforma, ms jugados en ese dispositivo, `days` (`"AAAA-MM-DD"` → ms) y cuándo lo pasó. **Ligero**: es lo que debe leer un Diario. Cada dispositivo escribe solo lo suyo y borra lo de los juegos que ya no tiene. Las horas del PC NO van aquí: están en `suite/lib/<PC>` (`m`, minutos), que ya publica Atrio del PC; en `days` sí van las partidas del PC que Carmesí vio empezar y acabar. | cada Carmesí |
| `nube/portadas/<kid>` | La portada que pusiste tú, en kDrive (`f.img`: id, nombre, hash, tamaño). | cualquier Carmesí |
| `nube/roms/<kid>`, `nube/guardados/<kid>` | La ROM y las partidas guardadas en kDrive. | cualquier Carmesí |

## Cómo leer el tiempo para un Diario

Para cada juego, lee `tiempo/<kid>/*` y **suma** los dispositivos (cada uno trae solo lo suyo; no hay nada repetido). Con eso salen los
minutos de cada día y por juego. Las horas que vinieron de Atrio al importar cuentan como del dispositivo que importó.

## La valoración

En Carmesí no se valora: la casilla «En Azabache» de la ficha solo LEE la general (0–100) y la reseña de Atrio/Azabache, enlazadas por
`juego:<título>` (y por los alias). Azabache es la dueña de la valoración.

## Lo que aún no está (se añadirá cuando Azabache lo pida)

- Las capturas del PC se quedan en el móvil (y en la galería): no se publican aparte de lo que ya hace Atrio del PC en `suite/shots`.
