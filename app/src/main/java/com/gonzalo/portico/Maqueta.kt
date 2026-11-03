package com.gonzalo.portico

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/*
 * La maqueta de la ficha (la misma idea que la de Atrio, con menos casillas): la ficha es una lista de filas; cada fila tiene
 * una casilla a todo lo ancho o dos separadas por una línea que tú colocas (⇔), y su alto también se arrastra (⇕).
 * En cada casilla eliges qué va: horas jugadas, veces y última vez, estado (terminado), valoración, apuntes, cómo se abre,
 * un texto libre o una imagen tuya. Se guarda en cada juego, y puedes usar el diseño de uno para todos los de su tipo.
 */

/** Una casilla: qué módulo enseña, o un texto libre (título y texto) o una imagen (ruta de un archivo de la app). */
data class MagCell(val type: String, val title: String = "", val text: String = "", val image: String = "")

/** Una fila: una o dos casillas; [split] es dónde va la línea (lo que ocupa la de la izquierda, 0–1); [height] el alto en dp (0: el que pida el contenido). */
data class MagRowSpec(val cells: List<MagCell>, val split: Float = 0.5f, val height: Float = 0f)

object MagLayouts {
    /** Los módulos que se pueden poner en una casilla (aparte de texto libre, imagen y vacía). */
    val MODULOS = listOf("horas", "veces", "estado", "valoracion", "lanzamiento")

    /** El diseño de siempre. */
    fun defaults(tipo: Tipo): List<MagRowSpec> = listOf(
        MagRowSpec(listOf(MagCell("horas"), MagCell("veces"))),
        MagRowSpec(listOf(MagCell("estado"), MagCell("valoracion"))),
        MagRowSpec(listOf(MagCell("lanzamiento"))),
    )

    fun aJson(rows: List<MagRowSpec>): String = JSONArray().apply {
        rows.forEach { r ->
            put(JSONObject().put("s", r.split.toDouble()).put("a", r.height.toDouble()).put("c", JSONArray().apply {
                r.cells.forEach { c -> put(JSONObject().put("t", c.type).put("h", c.title).put("x", c.text).put("i", c.image)) }
            }))
        }
    }.toString()

    fun deJson(s: String?): List<MagRowSpec>? = try {
        if (s.isNullOrBlank()) null else {
            val a = JSONArray(s)
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i); val cs = o.getJSONArray("c")
                MagRowSpec((0 until cs.length()).map { j -> cs.getJSONObject(j).let { c -> MagCell(c.optString("t", "vacio"), c.optString("h"), c.optString("x"), c.optString("i")) } }.take(2).ifEmpty { listOf(MagCell("vacio")) },
                    o.optDouble("s", 0.5).toFloat().coerceIn(0.3f, 0.7f), o.optDouble("a", 0.0).toFloat().coerceIn(0f, 1400f))
            }
        }
    } catch (e: Exception) { null }

    private fun prefs(c: Context) = c.getSharedPreferences("portico_maqueta", Context.MODE_PRIVATE)

    /** El diseño de este juego; si no tiene, el de su tipo; si no, el de siempre. */
    fun cargar(c: Context, j: Juego): List<MagRowSpec> = deJson(j.maqueta) ?: deJson(prefs(c).getString("tpl:" + j.tipo.name, null)) ?: defaults(j.tipo)

    /** Para todos los juegos del tipo: no se copian los textos ni las imágenes de este juego, solo dónde va cada cosa. */
    fun guardarPlantilla(c: Context, tipo: Tipo, rows: List<MagRowSpec>) {
        val limpio = rows.map { r -> r.copy(cells = r.cells.map { if (it.type == "texto" || it.type == "imagen") MagCell("vacio") else it }) }
        prefs(c).edit().putString("tpl:" + tipo.name, aJson(limpio)).apply()
    }
    fun quitarPlantilla(c: Context, tipo: Tipo) { prefs(c).edit().remove("tpl:" + tipo.name).apply() }
}

/** Un módulo que puede ir en una casilla: su antetítulo, una acción a la derecha (opcional) y lo que pinta. */
class MagDef(val label: String, val action: Pair<String, () -> Unit>? = null, val content: @Composable ColumnScope.() -> Unit)

val MAG_NOMBRES = mapOf(
    "horas" to "Horas jugadas", "veces" to "Veces y última vez", "estado" to "Estado", "valoracion" to "Valoración",
    "lanzamiento" to "Cómo se abre"
)

@Composable
fun MagLabel(t: String, accion: String? = null, alHacer: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { Antetitulo(t, tam = 12.sp, conCuadro = false) }
        if (accion != null && alHacer != null) Text(accion.uppercase(), color = Acento, fontFamily = Body, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.2.sp,
            modifier = Modifier.clickable(onClick = alHacer).padding(4.dp))
    }
}

@Composable
private fun Herramienta(t: String, activa: Boolean = true, acento: Boolean = false, alHacer: () -> Unit) =
    Text(t, color = if (!activa) Line else if (acento) Acento else Charcoal, fontFamily = Body, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.2.sp,
        modifier = Modifier.then(if (activa) Modifier.clickable(onClick = alHacer) else Modifier).padding(vertical = 6.dp))

@Composable
private fun Opcion2(t: String, activa: Boolean, alHacer: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = alHacer).padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(t, color = if (activa) Acento else Charcoal, fontFamily = Body, fontWeight = if (activa) FontWeight.Bold else FontWeight.Normal, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (activa) Text("✓", color = Acento, fontWeight = FontWeight.Bold)
    }
}

/**
 * La ficha en filas. En lectura, las filas con sus líneas; en edición ([disenando]) cada casilla se toca para cambiarla,
 * la línea ⇔ y el alto ⇕ se arrastran, y las filas se suben, bajan, dividen, unen, añaden o quitan.
 */
@Composable
fun MagPage(juego: Juego, defs: Map<String, MagDef>, disenando: Boolean) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var rows by remember(juego.id) { mutableStateOf(MagLayouts.cargar(ctx, juego)) }
    fun poner(n: List<MagRowSpec>) { rows = n; Biblioteca.cambiar(juego.id) { it.copy(maqueta = MagLayouts.aJson(n)) } }
    var eligiendo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var editandoTexto by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var imagenPara by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var internetPara by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var galeriaPara by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var aviso by remember { mutableStateOf("") }

    fun visible(c: MagCell) = c.type == "imagen" || c.type == "vacio" || defs.containsKey(c.type)
    fun celda(p: Pair<Int, Int>) = rows.getOrNull(p.first)?.cells?.getOrNull(p.second)
    fun ponCelda(p: Pair<Int, Int>, c: MagCell) = poner(rows.mapIndexed { i, r -> if (i != p.first) r else r.copy(cells = r.cells.mapIndexed { j, x -> if (j == p.second) c else x }) })

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u ->
        val p = galeriaPara; galeriaPara = null
        if (u != null && p != null) scope.launch { guardarPortada(ctx, juego.id + "-m", u)?.let { ponCelda(p, MagCell("imagen", image = it)) } ?: run { aviso = "No se ha podido usar esa imagen." } }
    }

    @Composable
    fun ColumnScope.Vista(c: MagCell) {
        when (c.type) {
            "texto" -> {
                if (c.title.isNotBlank()) MagLabel(c.title)
                if (c.text.isNotBlank()) Text(c.text, color = Charcoal, fontFamily = Display, fontSize = 17.sp, lineHeight = 25.sp)
                else if (disenando) Text("Texto libre", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 16.sp)
            }
            "imagen" -> {
                if (c.title.isNotBlank()) MagLabel(c.title)
                Box(Modifier.fillMaxWidth().weight(1f, fill = true).heightIn(min = 120.dp).clipToBounds().background(if (c.image.isBlank()) Sand else Color.Transparent)) {
                    if (c.image.isNotBlank()) AsyncImage(File(c.image), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                    else if (disenando) Text("Imagen", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 16.sp, modifier = Modifier.align(Alignment.Center))
                }
            }
            "vacio" -> if (disenando) Text("Vacía: toca para elegir", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 15.sp)
            else -> defs[c.type]?.let { d -> MagLabel(d.label, d.action?.first, d.action?.second); d.content(this) }
        }
    }

    rows.forEachIndexed { i, r ->
        val visibles = r.cells.withIndex().filter { visible(it.value) }
        if (visibles.isEmpty() && !disenando) return@forEachIndexed
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(Charcoal))
            if (disenando) {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("FILA ${i + 1}", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp, modifier = Modifier.weight(1f))
                    Herramienta("↑", i > 0) { poner(rows.toMutableList().also { val t = it[i]; it[i] = it[i - 1]; it[i - 1] = t }) }
                    Herramienta("↓", i < rows.size - 1) { poner(rows.toMutableList().also { val t = it[i]; it[i] = it[i + 1]; it[i + 1] = t }) }
                    if (r.cells.size == 1) Herramienta("DIVIDIR") { poner(rows.mapIndexed { k, x -> if (k == i) x.copy(cells = x.cells + MagCell("vacio"), split = 0.5f) else x }) }
                    else Herramienta("UNIR") { poner(rows.mapIndexed { k, x -> if (k == i) x.copy(cells = listOf(x.cells.firstOrNull { it.type != "vacio" } ?: x.cells[0])) else x }) }
                    if (r.height > 0f) Herramienta("ALTO AUTO") { poner(rows.mapIndexed { k, x -> if (k == i) x.copy(height = 0f) else x }) }
                    Herramienta("QUITAR", acento = true) { poner(rows.filterIndexed { k, _ -> k != i }) }
                }
            }
            var anchoPx by remember { mutableStateOf(1) }
            var altoPx by remember { mutableStateOf(1) }
            val densidad = LocalDensity.current.density
            val mostradas = if (disenando) r.cells.withIndex().toList() else visibles
            val fija = r.height > 0f
            Row(
                Modifier.fillMaxWidth().then(if (fija) Modifier.height(r.height.dp) else Modifier.height(IntrinsicSize.Min))
                    .onSizeChanged { anchoPx = it.width.coerceAtLeast(1); altoPx = it.height.coerceAtLeast(1) }
            ) {
                mostradas.forEachIndexed { n, (j, c) ->
                    val w = if (mostradas.size == 2) (if (n == 0) r.split else 1f - r.split) else 1f
                    if (n == 1) {
                        // La línea: en edición se arrastra
                        Box(
                            Modifier.fillMaxHeight().width(if (disenando) 28.dp else 1.5.dp)
                                .then(if (disenando) Modifier.semantics { contentDescription = "Línea de la fila ${i + 1}" }.pointerInput(i, rows.size) {
                                    detectHorizontalDragGestures(onDragEnd = { Biblioteca.cambiar(juego.id) { it.copy(maqueta = MagLayouts.aJson(rows)) } }) { ch, dx ->
                                        ch.consume()
                                        val ns = (rows[i].split + dx / anchoPx).coerceIn(0.3f, 0.7f)
                                        rows = rows.mapIndexed { k, x -> if (k == i) x.copy(split = ns) else x }
                                    }
                                } else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.fillMaxHeight().width(1.5.dp).background(Charcoal))
                            if (disenando) Box(Modifier.size(26.dp).clip(CircleShape).background(Acento), contentAlignment = Alignment.Center) { Text("⇔", color = SobreAcento, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Box(Modifier.weight(w).fillMaxHeight().clipToBounds()) {
                        Column(Modifier.fillMaxWidth().fillMaxHeight().padding(top = 12.dp, bottom = 18.dp, start = if (n == 1) 12.dp else 0.dp, end = if (n == 0 && mostradas.size == 2) 12.dp else 0.dp)) { Vista(c) }
                        if (disenando) Box(
                            Modifier.matchParentSize()
                                .drawBehind { drawRoundRect(Color(Skin.tones.general or 0xFF000000), style = androidx.compose.ui.graphics.drawscope.Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))) }
                                .clickable(onClickLabel = "Cambiar") { eligiendo = i to j }
                                .semantics { contentDescription = "Casilla " + when (c.type) { "texto" -> c.title.ifBlank { "Texto libre" }; "imagen" -> c.title.ifBlank { "Imagen" }; "vacio" -> "vacía"; else -> MAG_NOMBRES[c.type] ?: c.type } }
                        )
                    }
                }
            }
            if (disenando) {
                // El alto de la fila: se arrastra el círculo ⇕
                Box(
                    Modifier.fillMaxWidth().height(30.dp).semantics { contentDescription = "Alto de la fila ${i + 1}" }.pointerInput(i, rows.size) {
                        detectVerticalDragGestures(onDragEnd = { Biblioteca.cambiar(juego.id) { it.copy(maqueta = MagLayouts.aJson(rows)) } }) { ch, dy ->
                            ch.consume()
                            val base = if (rows[i].height > 0f) rows[i].height else altoPx / densidad
                            val nh = (base + dy / densidad).coerceIn(56f, 1400f)
                            rows = rows.mapIndexed { k, x -> if (k == i) x.copy(height = nh) else x }
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                    Box(Modifier.size(26.dp).clip(CircleShape).background(Acento), contentAlignment = Alignment.Center) { Text("⇕", color = SobreAcento, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
    if (disenando) {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(Charcoal))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) { Herramienta("+ AÑADIR FILA", acento = true) { poner(rows + MagRowSpec(listOf(MagCell("vacio")))); eligiendo = rows.size - 1 to 0 } }
            val de = when (juego.tipo) { Tipo.ROM -> "DE ROMS"; Tipo.APP -> "DE ANDROID"; Tipo.WEB -> "WEB" }
            Herramienta("USAR ESTE DISEÑO EN TODOS LOS JUEGOS $de") { MagLayouts.guardarPlantilla(ctx, juego.tipo, rows); aviso = "Guardado: lo verás en los demás juegos ${de.lowercase().replace("de roms", "de ROMs")} (sin tus textos ni imágenes)." }
            Herramienta("RESTABLECER EL DISEÑO DE SIEMPRE") { MagLayouts.quitarPlantilla(ctx, juego.tipo); poner(MagLayouts.defaults(juego.tipo)); aviso = "Diseño de siempre." }
            if (aviso.isNotBlank()) Cuerpo(aviso, tam = 13, modifier = Modifier.padding(top = 6.dp))
        }
    }

    // ¿Qué va en esta casilla?
    eligiendo?.let { p ->
        val cur = celda(p)
        AlertDialog(
            onDismissRequest = { eligiendo = null }, containerColor = Crudo,
            title = { Text("¿Qué va aquí?", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (cur?.type == "imagen") { Opcion2("Cambiar la imagen", false) { eligiendo = null; imagenPara = p }; Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(Line)) }
                    MagLayouts.MODULOS.filter { it in defs }.forEach { t -> Opcion2(MAG_NOMBRES[t] ?: t, cur?.type == t) { ponCelda(p, MagCell(t)); eligiendo = null } }
                    Opcion2("Imagen", cur?.type == "imagen") { eligiendo = null; imagenPara = p }
                    Opcion2("Vacía", cur?.type == "vacio") { ponCelda(p, MagCell("vacio")); eligiendo = null }
                }
            },
            confirmButton = { TextButton({ eligiendo = null }) { Text("Cerrar", color = Charcoal) } }
        )
    }
    editandoTexto?.let { p ->
        val cur = celda(p)
        var titulo by remember(p) { mutableStateOf(if (cur?.type == "texto") cur.title else "") }
        var texto by remember(p) { mutableStateOf(if (cur?.type == "texto") cur.text else "") }
        AlertDialog(
            onDismissRequest = { editandoTexto = null }, containerColor = Crudo,
            title = { Text("Texto libre", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(titulo, { titulo = it }, label = { Text("Título (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(texto, { texto = it }, label = { Text("Texto") }, modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp))
                }
            },
            confirmButton = { TextButton({ ponCelda(p, MagCell("texto", titulo.trim(), texto.trim())); editandoTexto = null }) { Text("Guardar", color = Acento) } },
            dismissButton = { TextButton({ editandoTexto = null }) { Text("Cancelar", color = Charcoal) } }
        )
    }
    imagenPara?.let { p ->
        AlertDialog(
            onDismissRequest = { imagenPara = null }, containerColor = Crudo,
            title = { Text("Imagen", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonSuave("Elegir de la galería", Icons.Filled.PhotoLibrary, Modifier.fillMaxWidth()) { imagenPara = null; galeriaPara = p; galeria.launch("image/*") }
                    BotonSuave("Buscar en internet", Icons.Filled.Search, Modifier.fillMaxWidth()) { imagenPara = null; internetPara = p }
                    Cuerpo("Se guarda en este móvil. Si la casilla es más alta o más ancha, la imagen se recorta para llenarla.", tam = 12)
                }
            },
            confirmButton = {}, dismissButton = { TextButton({ imagenPara = null }) { Text("Cancelar", color = Charcoal) } }
        )
    }
    internetPara?.let { p ->
        InternetPicker(initialQuery = juego.titulo, idJuego = juego.id + "-m", onPick = { ruta -> ponCelda(p, MagCell("imagen", image = ruta)); internetPara = null }, onDismiss = { internetPara = null })
    }
}
