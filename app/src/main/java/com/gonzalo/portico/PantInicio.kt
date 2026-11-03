package com.gonzalo.portico

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
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
fun metaCorta(j: Juego): String {
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
 * La pantalla de inicio de Pórtico, como la portada de Atrio: cabecera de revista, el menú de secciones (Recientes, Favoritos, ROMs…) y,
 * debajo, tu lienzo: casillas invisibles donde colocas juegos e imágenes con el tamaño y el color que quieras (se sincroniza entre tus móviles).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PantInicio(
    alAbrir: (Juego) -> Unit, alJugar: (Juego) -> Unit, alAnadir: (Tipo?) -> Unit,
    alIr: (Destino) -> Unit, alBuscar: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val hoy = remember { Calendar.getInstance() }
    val atrio = remember { runCatching { ctx.packageManager.getLaunchIntentForPackage(ATRIO) }.getOrNull() }
    var editando by rememberSaveable { mutableStateOf(false) }
    var elegido by remember { mutableStateOf<Elemento?>(null) }
    var anadiendo by remember { mutableStateOf(false) }
    LaunedInicial()

    val imagen = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u ->
        if (u != null) scope.launch {
            val id = Pantalla.nuevoId()
            guardarImagenPantalla(ctx, id, u)?.let { nombre ->
                val (x, y) = Pantalla.hueco(2, 2)
                Pantalla.poner(Elemento(id, Elemento.IMAGEN, nombre, x, y, 2, 2))
            }
        }
    }

    val secciones = listOf<Pair<String, () -> Unit>>(
        "Recientes" to { alIr(Destino.Recientes) }, "Favoritos" to { alIr(Destino.Favoritos) }, "ROMs" to { alIr(Destino.Roms) },
        "Android" to { alIr(Destino.Android) }, "Web" to { alIr(Destino.Web) }, "Todo" to { alIr(Destino.Juegos) }, "Sistemas" to { alIr(Destino.Sistemas) }
    )

    Column(Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            EnlaceRevista("Buscar") { alBuscar() }
            EnlaceRevista("Añadir") { alAnadir(null) }
            EnlaceRevista(if (editando) "Listo" else "Editar", acento = editando) { editando = !editando }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.Settings, "Ajustes", tint = Charcoal, modifier = Modifier.combinedClickable(onClick = { alIr(Destino.Ajustes) }).padding(8.dp).size(22.dp))
        }
        Cabecera(64.sp, Modifier.padding(top = 2.dp))
        FileteCabecera(3.dp)
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            Text("Nº ${hoy.get(Calendar.DAY_OF_YEAR)}", color = Charcoal, fontFamily = Body, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            Text(SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES")).format(hoy.time).uppercase(), color = Charcoal, fontFamily = Body, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Filete(1.dp)
        // Las secciones, como el menú de la portada de Atrio: versalitas entre dos filetes
        FlowRow(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            secciones.forEach { (t, f) ->
                Text(t.uppercase(), color = Charcoal, fontFamily = Body, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, maxLines = 1,
                    modifier = Modifier.clickable(onClick = f).padding(vertical = 9.dp))
            }
        }
        Filete(1.dp)
        Spacer(Modifier.height(10.dp))

        if (editando) {
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Arrastra para mover, el círculo ⤡ de la esquina para el tamaño, toca para el color.", color = Muted, fontFamily = Body, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.weight(1f))
                EnlaceRevista("+ Añadir", acento = true) { anadiendo = true }
            }
        }
        Lienzo(editando, alJugar = alJugar, alAbrir = alAbrir, alEditar = { elegido = it })
        if (Pantalla.items.isEmpty() && !editando) {
            Column {
                Text(if (Biblioteca.juegos.isEmpty()) "Pórtico no es un emulador: es la puerta desde la que abres los que ya tienes, los juegos de Android y los de navegador." else "Tu pantalla está vacía: toca «Editar» y añade juegos o imágenes donde quieras.", color = Muted, fontFamily = Body, fontSize = 15.sp, lineHeight = 22.sp)
                if (Biblioteca.juegos.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    MagLink("ROMs y emuladores", "GBA, DS, SNES, PSP… con el emulador que elijas", accent = true) { alAnadir(Tipo.ROM) }
                    Filete(1.dp, Line)
                    MagLink("Juegos de Android", "Los que ya tienes instalados") { alAnadir(Tipo.APP) }
                    Filete(1.dp, Line)
                    MagLink("Juegos de navegador", "PokéRogue y otros, a pantalla completa") { alAnadir(Tipo.WEB) }
                }
            }
        }
        Column(Modifier.padding(top = 10.dp)) {
            if (!editando && Pantalla.items.isNotEmpty()) Text("Un toque juega · mantén pulsado para ver la ficha", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            if (atrio != null) {
                Spacer(Modifier.height(10.dp))
                Filete(1.dp, Line)
                MagLink("Abrir Atrio", "Valora lo que acabas de jugar") { ctx.startActivity(atrio.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            Text("Pórtico · hecho en casa", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp))
            Spacer(Modifier.height(24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
        }
    }

    elegido?.let { e -> DialogoElemento(e, alCerrar = { elegido = null }, alFicha = { clave -> Biblioteca.juegos.firstOrNull { it.clave == clave }?.let { alAbrir(it) } }) }
    if (anadiendo) DialogoAnadirElemento(alCerrar = { anadiendo = false }, alImagen = { anadiendo = false; imagen.launch("image/*") })
}

@Composable
private fun LaunedInicial() { val c = LocalContext.current; LaunchedEffect(Unit) { Pantalla.inicial(c) } }

/** Color y quitar (y la ficha, si es un juego) de un elemento del lienzo. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoElemento(e: Elemento, alCerrar: () -> Unit, alFicha: (String) -> Unit) {
    val actual = Pantalla.items.firstOrNull { it.id == e.id } ?: e
    AlertDialog(
        onDismissRequest = alCerrar, containerColor = Paper, shape = esquina(4.dp),
        title = { Text(if (e.esJuego) (Biblioteca.juegos.firstOrNull { it.clave == e.ref }?.titulo ?: "Juego") else "Imagen", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Etiqueta("Color de fondo")
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    COLORES_ELEMENTO.forEach { c ->
                        val col = if (c == 0) Crudo else Color(c)
                        Box(Modifier.size(34.dp).clip(CircleShape).background(col).border(if (actual.color == c) 3.dp else 1.dp, if (actual.color == c) Acento else Line, CircleShape)
                            .clickable { Pantalla.poner(actual.copy(color = c)) }, contentAlignment = Alignment.Center) {
                            if (c == 0) Text("T", color = Charcoal, fontFamily = Body, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
                Text("«T» es el color del tema.", color = Muted, fontFamily = Body, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                if (e.esJuego) { Spacer(Modifier.height(8.dp)); MagLink("Ver la ficha") { alCerrar(); alFicha(e.ref) } }
            }
        },
        confirmButton = { TextButton({ alCerrar() }) { Text("LISTO", color = Acento, fontFamily = Body, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp) } },
        dismissButton = { TextButton({ Pantalla.quitar(e.id); alCerrar() }) { Text("QUITAR", color = Charcoal, fontFamily = Body, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp) } }
    )
}

/** Añadir al lienzo: un juego de tu biblioteca o una imagen. */
@Composable
private fun DialogoAnadirElemento(alCerrar: () -> Unit, alImagen: () -> Unit) {
    var busca by remember { mutableStateOf("") }
    val q = Catalogo.norm(busca)
    val lista = remember(busca) { recientes(Biblioteca.juegos.toList()).filter { q.isEmpty() || Catalogo.norm(it.titulo).contains(q) }.take(40) }
    AlertDialog(
        onDismissRequest = alCerrar, containerColor = Paper, shape = esquina(4.dp),
        title = { Text("Añadir a la pantalla", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                MagLink("Una imagen", "De tu galería, del tamaño que quieras") { alImagen() }
                Filete(1.dp, Line)
                CampoBusqueda(busca) { busca = it }
                Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    lista.forEach { j ->
                        Text(j.titulo, color = Charcoal, fontFamily = Display, fontSize = 18.sp, maxLines = 1,
                            modifier = Modifier.fillMaxWidth().clickable {
                                val (x, y) = Pantalla.hueco(2, 1); Pantalla.poner(Elemento(Pantalla.nuevoId(), Elemento.JUEGO, j.clave, x, y, 2, 1)); alCerrar()
                            }.padding(vertical = 10.dp))
                        Filete(1.dp, Line)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(alCerrar) { Text("CERRAR", color = Charcoal, fontFamily = Body, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp) } }
    )
}
