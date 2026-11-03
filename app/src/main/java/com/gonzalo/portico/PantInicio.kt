package com.gonzalo.portico

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Ordena los juegos: los últimos que jugaste primero y, si no, los últimos que añadiste. */
fun recientes(l: List<Juego>) = l.sortedWith(compareByDescending<Juego> { it.ultima }.thenByDescending { it.anadido })

const val ATRIO = "com.gonzalo.gamerate"

/** Una frase como «Juegos · GBA · 3 h 20 min». */
fun metaDe(j: Juego): String = listOf(
    when (j.tipo) { Tipo.ROM -> Catalogo.sistema(j.sistema)?.nombre ?: j.sistema; Tipo.APP -> "Android"; Tipo.WEB -> "Web" },
    if (j.msJugados >= 60_000) tiempoDe(j.msJugados) else ""
).filter { it.isNotBlank() }.joinToString(" · ")

/** La imagen de un juego: la que elegiste, la de libretro, el icono de la app o el degradado con su inicial. */
@Composable
fun ImagenJuego(j: Juego, modifier: Modifier = Modifier, tamLetra: Int = 64) {
    PortadaArco(j, modifier, titulo = false, letra = true, relleno = true, tamLetra = tamLetra)
}

/** Lo de debajo del nombre en una baldosa: «GBA · 3 h». */
private fun metaCorta(j: Juego): String {
    val h = if (j.terminado) "terminado" else if (j.msJugados >= 60_000) tiempoDe(j.msJugados).replace(" min", " m").replace(" h ", " h ") else ""
    return listOf(etiquetaDe(j), if (j.enOtroDispositivo) "en otro móvil" else h).filter { it.isNotBlank() }.joinToString(" · ")
}

/**
 * Una baldosa del launcher: la misma disposición que los widgets (portada a la izquierda, el nombre a la derecha), con el papel
 * de la revista y su reborde fino de tinta, metido hacia dentro. Un toque juega; mantenerla pulsada abre la ficha.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Baldosa(j: Juego, alJugar: () -> Unit, alAbrir: () -> Unit, modifier: Modifier = Modifier) {
    val hf = LocalHapticFeedback.current
    val tinta = Charcoal
    Row(
        modifier.height(92.dp).clip(RoundedCornerShape(18.dp)).background(Crudo)
            .combinedClickable(onClick = alJugar, onLongClick = { hf.performHapticFeedback(HapticFeedbackType.LongPress); alAbrir() })
            .drawBehind {
                // El reborde de los widgets: una línea fina del color del texto, metida hacia dentro
                val m = 3.5.dp.toPx()
                drawRoundRect(tinta, Offset(m, m), Size(size.width - 2 * m, size.height - 2 * m), CornerRadius(14.dp.toPx()), style = Stroke(1.dp.toPx()))
            }.padding(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.fillMaxHeight().aspectRatio(0.68f).clip(RoundedCornerShape(8.dp))) { ImagenJuego(j, Modifier.matchParentSize(), tamLetra = 30) }
        Column(Modifier.weight(1f).fillMaxHeight().padding(start = 9.dp), verticalArrangement = Arrangement.SpaceBetween) {
            FitTitle(j.titulo, 15.sp, 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 3, minSize = 10f, letterSpacing = (-0.2).sp)
            Text(metaCorta(j).uppercase(), color = Acento, fontFamily = Body, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp, maxLines = 1)
        }
    }
}

/** La baldosa de «añadir»: el mismo marco, a trazos, con un «+». */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BaldosaAnadir(alAnadir: () -> Unit, modifier: Modifier = Modifier) {
    val tinta = Muted
    Row(
        modifier.height(92.dp).clip(RoundedCornerShape(18.dp)).combinedClickable(onClick = alAnadir)
            .drawBehind {
                val m = 3.5.dp.toPx()
                drawRoundRect(tinta, Offset(m, m), Size(size.width - 2 * m, size.height - 2 * m), CornerRadius(14.dp.toPx()),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
            }.padding(9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Add, null, tint = Acento, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(8.dp))
        Text("AÑADIR", color = Charcoal, fontFamily = Body, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
    }
}

/**
 * La pantalla de inicio de Pórtico: un launcher. Cabecera de revista, pestañas (Recientes, Favoritos, ROMs, Android, Web, Todo)
 * y una rejilla de baldosas con lo que toca. Un toque juega; mantener pulsado abre la ficha.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PantInicio(
    alAbrir: (Juego) -> Unit, alJugar: (Juego) -> Unit, alAnadir: (Tipo?) -> Unit,
    alIr: (Destino) -> Unit, alBuscar: () -> Unit
) {
    val ctx = LocalContext.current
    val todos = Biblioteca.juegos.toList()
    val hoy = remember { Calendar.getInstance() }
    val atrio = remember { runCatching { ctx.packageManager.getLaunchIntentForPackage(ATRIO) }.getOrNull() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sistema by rememberSaveable { mutableStateOf<String?>(null) }
    var buscando by rememberSaveable { mutableStateOf(false) }
    var busca by rememberSaveable { mutableStateOf("") }
    val foco = remember { FocusRequester() }
    LaunchedEffect(buscando) { if (buscando) runCatching { foco.requestFocus() } }

    val pestanas = listOf("Recientes", "Favoritos", "ROMs", "Android", "Web", "Todo")
    val cuentas = listOf(todos.count { it.ultima > 0 }, todos.count { it.favorito }, todos.count { it.tipo == Tipo.ROM }, todos.count { it.tipo == Tipo.APP }, todos.count { it.tipo == Tipo.WEB }, todos.size)
    val q = Catalogo.norm(busca)
    val base = when (tab) {
        0 -> recientes(todos.filter { it.ultima > 0 }.ifEmpty { todos })
        1 -> todos.filter { it.favorito }.sortedBy { it.titulo.lowercase() }
        2 -> todos.filter { it.tipo == Tipo.ROM && (sistema == null || it.sistema == sistema) }.sortedBy { it.titulo.lowercase() }
        3 -> todos.filter { it.tipo == Tipo.APP }.sortedBy { it.titulo.lowercase() }
        4 -> todos.filter { it.tipo == Tipo.WEB }.sortedBy { it.titulo.lowercase() }
        else -> todos.sortedBy { it.titulo.lowercase() }
    }
    // (buscando, se mira en todo lo que tienes, no solo en la pestaña)
    val lista = if (q.isEmpty()) base else todos.filter { Catalogo.norm(it.titulo).contains(q) || Catalogo.norm(etiquetaDe(it)).contains(q) }.sortedBy { it.titulo.lowercase() }
    val sistemas = todos.filter { it.tipo == Tipo.ROM }.groupBy { it.sistema }.entries.sortedByDescending { it.value.size }

    LazyVerticalGrid(
        GridCells.Adaptive(150.dp), Modifier.fillMaxSize().background(Paper),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    EnlaceRevista("Buscar") { buscando = !buscando; if (!buscando) busca = "" }
                    EnlaceRevista("Lista") { alIr(Destino.Juegos) }
                    EnlaceRevista("Sistemas") { alIr(Destino.Sistemas) }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Filled.Settings, "Ajustes", tint = Charcoal, modifier = Modifier.combinedClickable(onClick = { alIr(Destino.Ajustes) }).padding(8.dp).size(22.dp))
                }
                // Cabecera de revista, compacta: es un launcher, lo que importa son las baldosas
                Cabecera(64.sp, Modifier.padding(top = 2.dp))
                FileteCabecera(3.dp)
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text("Nº ${hoy.get(Calendar.DAY_OF_YEAR)}", color = Charcoal, fontFamily = Body, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.weight(1f))
                    Text(SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES")).format(hoy.time).uppercase(), color = Charcoal, fontFamily = Body, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Filete(1.dp)
                PestanasRevista(pestanas, tab, cuentas = cuentas) { tab = it; sistema = null }
                if (tab == 2 && sistemas.size > 1) FiltrosRevista(listOf("Todos") + sistemas.map { (Catalogo.sistema(it.key)?.corto ?: it.key) + " " + it.value.size }, sistemas.indexOfFirst { it.key == sistema } + 1) { i ->
                    sistema = if (i == 0) null else sistemas[i - 1].key
                }
                if (buscando) CampoBusquedaRevista(busca, "Buscar en todos tus juegos", foco) { busca = it }
                Spacer(Modifier.height(12.dp))
                if (todos.isEmpty()) {
                    Antetitulo("Para empezar", Modifier.padding(bottom = 8.dp))
                    Text("Pórtico no es un emulador: es la puerta desde la que abres los que ya tienes, los juegos de Android y los de navegador.", color = Muted, fontFamily = Body, fontSize = 15.sp, lineHeight = 22.sp)
                    Spacer(Modifier.height(6.dp))
                    MagLink("ROMs y emuladores", "GBA, DS, SNES, PSP… con el emulador que elijas", accent = true) { alAnadir(Tipo.ROM) }
                    Filete(1.dp, Line)
                    MagLink("Juegos de Android", "Los que ya tienes instalados") { alAnadir(Tipo.APP) }
                    Filete(1.dp, Line)
                    MagLink("Juegos de navegador", "PokéRogue y otros, a pantalla completa") { alAnadir(Tipo.WEB) }
                    Spacer(Modifier.height(6.dp))
                } else if (lista.isEmpty()) {
                    Text(if (q.isNotEmpty()) "Nada con esa búsqueda." else if (tab == 1) "Aún no tienes favoritos: márcalos con ★ en su ficha." else "Aquí no hay nada todavía.", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 17.sp, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
        items(lista, key = { it.id }) { j -> Baldosa(j, alJugar = { alJugar(j) }, alAbrir = { alAbrir(j) }) }
        item { BaldosaAnadir({ alAnadir(null) }) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 10.dp)) {
                if (todos.isNotEmpty()) Text("Un toque juega · mantén pulsado para ver la ficha", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                if (atrio != null) {
                    Spacer(Modifier.height(10.dp))
                    Filete(1.dp, Line)
                    MagLink("Abrir Atrio", "Valora lo que acabas de jugar") { ctx.startActivity(atrio.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                Text("Pórtico · hecho en casa", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp))
            }
        }
    }
}
