package com.gonzalo.portico

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/** Los colores que se pueden dar a un elemento (0 = el del tema). */
val COLORES_ELEMENTO = listOf(0, 0xFFC8362B.toInt(), 0xFFE08A2E.toInt(), 0xFFE3B53B.toInt(), 0xFF4F8A5B.toInt(), 0xFF2F5D8A.toInt(),
    0xFF6B4C9A.toInt(), 0xFFD66B9A.toInt(), 0xFF15120F.toInt(), 0xFFF3EEE4.toInt(), 0xFF8A7F72.toInt())

/** Tinta legible sobre un fondo. */
fun tintaSobre(fondo: Color): Color = if (fondo.luminance() > 0.45f) Color(0xFF15120F) else Color(0xFFF3EEE4)

/** Guarda una imagen elegida para la pantalla de inicio (reducida) y devuelve su nombre de archivo. */
suspend fun guardarImagenPantalla(c: Context, id: String, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        var s = 1; while (o.outWidth / s > 1600 || o.outHeight / s > 1600) s *= 2
        val b = c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s }) } ?: return@runCatching null
        val nombre = "$id.jpg"
        File(Pantalla.dirImagenes(c), nombre).outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        nombre
    }.getOrNull()
}

/** Lo que se está arrastrando en el lienzo: qué elemento, si se mueve o se estira, y cuánto (en píxeles). */
private class Arrastre(val id: String, val estirar: Boolean, val delta: Offset)

/**
 * El lienzo de la pantalla de inicio. Sin editar: un toque juega, mantener pulsado abre la ficha. Editando: arrastra un elemento para moverlo
 * (se encaja en las casillas), arrastra el círculo de su esquina para hacerlo grande o pequeño, y toca para elegir su color o quitarlo.
 * (En edición un solo detector de gestos, el del lienzo, decide a qué elemento le toca cada toque: así los elementos no compiten entre sí.)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Lienzo(editando: Boolean, alJugar: (Juego) -> Unit, alAbrir: (Juego) -> Unit, alEditar: (Elemento) -> Unit, modifier: Modifier = Modifier) {
    val items = Pantalla.items.toList()
    val juegos = Biblioteca.juegos.toList()
    val hf = LocalHapticFeedback.current
    val d = LocalDensity.current
    var arrastre by remember { mutableStateOf<Arrastre?>(null) }
    val visibles by rememberUpdatedState(items.filter { !it.esJuego || juegos.any { j -> j.clave == it.ref } })
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val celda = maxWidth / Pantalla.COLUMNAS
        val px = with(d) { celda.toPx() }
        val filas = maxOf(Pantalla.filas() + if (editando) 2 else 0, 2)
        val radioAsa = with(d) { 22.dp.toPx() }
        val margen = with(d) { 4.dp.toPx() }
        // A quién le toca un punto: el círculo de la esquina de un elemento (estirar) o su cuerpo (mover)
        fun golpe(p: Offset): Pair<Elemento, Boolean>? {
            val l = visibles
            l.forEach { e ->
                val c = Offset((e.x + e.w) * px - margen - with(d) { 19.dp.toPx() }, (e.y + e.h) * px - margen - with(d) { 19.dp.toPx() })
                if ((p - c).getDistance() <= radioAsa) return e to true
            }
            l.forEach { e -> if (p.x >= e.x * px && p.x < (e.x + e.w) * px && p.y >= e.y * px && p.y < (e.y + e.h) * px) return e to false }
            return null
        }
        // Las casillas se ven solo mientras editas
        val guias = Modifier.drawBehind {
            if (editando) {
                val r = 2.5.dp.toPx()
                for (cx in 0..Pantalla.COLUMNAS) for (cy in 0..filas) drawCircle(Color.Gray.copy(alpha = 0.45f), r, Offset(cx * px, cy * px))
            }
        }
        val gestos = if (!editando) Modifier else Modifier
            .pointerInput(px) { detectTapGestures(onTap = { p -> golpe(p)?.first?.let(alEditar) }) }
            .pointerInput(px) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val objetivo = golpe(down.position) ?: return@awaitEachGesture
                    var total = Offset.Zero
                    val primero = awaitTouchSlopOrCancellation(down.id) { ch, sobre -> ch.consume(); total += sobre } ?: return@awaitEachGesture
                    arrastre = Arrastre(objetivo.first.id, objetivo.second, total)
                    hf.performHapticFeedback(HapticFeedbackType.LongPress)
                    val fin = drag(primero.id) { ch -> total += ch.positionChangeIgnoreConsumed(); ch.consume(); arrastre = Arrastre(objetivo.first.id, objetivo.second, total) }
                    if (fin) {
                        val e = Pantalla.items.firstOrNull { it.id == objetivo.first.id }
                        if (e != null) {
                            val n = if (objetivo.second) e.copy(w = ((e.w * px + total.x) / px).roundToInt().coerceIn(1, Pantalla.COLUMNAS - e.x), h = ((e.h * px + total.y) / px).roundToInt().coerceIn(1, 12))
                            else e.copy(x = ((e.x * px + total.x) / px).roundToInt().coerceIn(0, Pantalla.COLUMNAS - e.w), y = ((e.y * px + total.y) / px).roundToInt().coerceAtLeast(0))
                            if (n != e) Pantalla.colocar(n)
                        }
                    }
                    arrastre = null
                }
            }
        Box(Modifier.fillMaxWidth().height(celda * filas).then(guias).then(gestos)) {
            for (e in items) {
                val j = if (e.esJuego) juegos.firstOrNull { it.clave == e.ref } else null
                if (!e.esJuego || j != null) key(e.id) {
                    val a = arrastre?.takeIf { it.id == e.id }
                    ElementoLienzo(e, j, px, editando, a, alJugar, alAbrir)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ElementoLienzo(e0: Elemento, j: Juego?, px: Float, editando: Boolean, a: Arrastre?, alJugar: (Juego) -> Unit, alAbrir: (Juego) -> Unit) {
    val d = LocalDensity.current
    val hf = LocalHapticFeedback.current
    val mueve = if (a != null && !a.estirar) a.delta else Offset.Zero
    val estira = if (a != null && a.estirar) a.delta else Offset.Zero
    val ancho = (e0.w * px + estira.x).coerceAtLeast(px * 0.8f)
    val alto = (e0.h * px + estira.y).coerceAtLeast(px * 0.8f)
    val margen = with(d) { 4.dp.toPx() }
    val fondo = if (e0.color != 0) Color(e0.color) else Crudo
    val tinta = if (e0.color != 0) tintaSobre(fondo) else Charcoal
    val forma = RoundedCornerShape(18.dp)
    val sinEditar: Modifier = if (!editando && j != null) Modifier.combinedClickable(onClick = { alJugar(j) }, onLongClick = { hf.performHapticFeedback(HapticFeedbackType.LongPress); alAbrir(j) }) else Modifier

    Box(
        Modifier
            .offset { IntOffset((e0.x * px + mueve.x + margen).roundToInt(), (e0.y * px + mueve.y + margen).roundToInt()) }
            .size(with(d) { (ancho - 2 * margen).toDp() }, with(d) { (alto - 2 * margen).toDp() })
            .testTag("el-" + e0.id)
            .zIndex(if (a != null) 2f else 0f)
            .shadow(if (a != null) 10.dp else 0.dp, forma)
            .clip(forma).background(fondo)
            .drawBehind {
                // El reborde de los widgets: una línea fina de tinta, metida hacia dentro
                val m = 3.5.dp.toPx()
                drawRoundRect(tinta, Offset(m, m), Size(size.width - 2 * m, size.height - 2 * m), CornerRadius(14.dp.toPx()), style = Stroke(1.dp.toPx()))
            }
            .then(sinEditar)
    ) {
        Contenido(e0, j, tinta)
        if (editando) Box(Modifier.align(Alignment.BottomEnd).padding(6.dp).size(26.dp).clip(CircleShape).background(Acento).testTag("asa-" + e0.id)) {
            Text("⤡", color = SobreAcento, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))
        }
    }
}

/** El mayor tamaño de letra (entre [min] y [max], en sp) con el que [texto] cabe entero en [ancho]×[alto] px, sin partir ninguna palabra. */
fun tamanoQueCabe(medidor: TextMeasurer, texto: String, estilo: TextStyle, ancho: Int, alto: Int, max: Float, min: Float): Float {
    if (ancho <= 0 || alto <= 0) return min
    val palabras = texto.split(' ', '\n').filter { it.isNotEmpty() }
    fun cabe(sp: Float): Boolean {
        val st = estilo.copy(fontSize = sp.sp, lineHeight = (sp * 1.08f).sp)
        if (palabras.any { medidor.measure(it, st.copy(textAlign = TextAlign.Center), softWrap = false).size.width > ancho }) return false
        val r = medidor.measure(texto, st.copy(textAlign = TextAlign.Center), constraints = Constraints(maxWidth = ancho))
        return r.size.height <= alto && !r.didOverflowHeight && !r.didOverflowWidth
    }
    var lo = min; var hi = max
    if (cabe(hi)) return hi
    if (!cabe(lo)) return lo
    repeat(14) { val m = (lo + hi) / 2f; if (cabe(m)) lo = m else hi = m }
    return lo
}

/** ¿Ha partido el texto alguna palabra por la mitad al pintarlo? (una línea acaba donde no hay espacio) */
fun partePalabras(r: androidx.compose.ui.text.TextLayoutResult, texto: String): Boolean {
    for (i in 0 until r.lineCount - 1) {
        val fin = r.getLineEnd(i, visibleEnd = false)
        if (fin in 1 until texto.length && !texto[fin - 1].isWhitespace() && !texto[fin].isWhitespace()) return true
    }
    return false
}

/** Un título centrado, tan grande como quepa en su hueco sin cortarse nunca (salvo que ni a la letra más pequeña quepa). */
@Composable
fun TituloAjustado(texto: String, color: Color, modifier: Modifier = Modifier, max: Float = 44f, min: Float = 3.5f, peso: FontWeight = FontWeight.SemiBold) {
    val medidor = rememberTextMeasurer()
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val w = constraints.maxWidth; val h = constraints.maxHeight
        val base = remember(peso) { TextStyle(fontFamily = Display, fontWeight = peso) }
        val ideal = remember(texto, w, h, peso, Skin.hashCode(), Display) { tamanoQueCabe(medidor, texto, base, w, h, max, min) * 0.97f }
        // Por si al pintar sale más grande de lo medido (la letra puede terminar de cargarse después): si se desborda, se achica hasta que quepa
        var factor by remember(texto, w, h, peso) { mutableStateOf(1f) }
        val sp = (ideal * factor).coerceAtLeast(min)
        Text(texto, color = color, fontFamily = Display, fontWeight = peso, fontSize = sp.sp, lineHeight = (sp * 1.08f).sp, textAlign = TextAlign.Center, softWrap = true,
            onTextLayout = { if ((it.didOverflowHeight || it.didOverflowWidth || it.size.height > h || partePalabras(it, texto)) && sp > min + 0.1f) factor *= 0.93f })
    }
}

/**
 * Una imagen encuadrada en su hueco, con total libertad: [zoom] (1 = rellena el hueco; menos, más pequeña; más, ampliada) y [ox]/[oy], lo que se
 * desplaza, como fracción del ancho y del alto del hueco (de -1 a 1; 0 = centrada).
 */
@Composable
fun ImagenEncuadrada(f: File, zoom: Float, ox: Float, oy: Float, modifier: Modifier = Modifier) {
    AsyncImage(f, null, modifier.graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = ox * size.width; translationY = oy * size.height }, contentScale = ContentScale.Crop)
}

private fun horasDe(j: Juego) = if (j.msJugados >= 60_000) tiempoDe(j.msJugados) else "0 h"

@Composable
private fun Contenido(e: Elemento, j: Juego?, tinta: Color) {
    val ctx = LocalContext.current
    if (e.esImagen) {
        val f = File(Pantalla.dirImagenes(ctx), e.ref)
        Box(Modifier.fillMaxSize().padding(7.dp).clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            if (f.exists()) ImagenEncuadrada(f, e.zoom, e.ox, e.oy, Modifier.fillMaxSize())
            else Text("imagen sin bajar", color = tinta.copy(alpha = 0.6f), fontSize = 11.sp)
        }
        return
    }
    if (j == null) return
    val realce = if (e.color == 0) Acento else tinta.copy(alpha = 0.9f)
    val horas = horasDe(j).uppercase()
    when {
        e.w == 1 && e.h == 1 -> Box(Modifier.fillMaxSize().padding(9.dp).clip(RoundedCornerShape(8.dp))) {
            ImagenJuego(j, Modifier.matchParentSize(), tamLetra = 26)
            Text(horas, color = Color.White, fontFamily = Body, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, maxLines = 1,
                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 6.dp, vertical = 2.dp))
        }
        e.h == 1 -> Row(Modifier.fillMaxSize().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.fillMaxHeight().aspectRatio(0.68f).clip(RoundedCornerShape(8.dp))) { ImagenJuego(j, Modifier.matchParentSize(), tamLetra = 30) }
            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(horas, color = realce, fontFamily = Body, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp, maxLines = 1)
                TituloAjustado(j.titulo, tinta, Modifier.weight(1f).fillMaxWidth())
            }
        }
        else -> Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(horas, color = realce, fontFamily = Body, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, maxLines = 1, modifier = Modifier.padding(bottom = 6.dp))
            Box(Modifier.weight(3f).fillMaxWidth().clip(RoundedCornerShape(10.dp))) { ImagenJuego(j, Modifier.matchParentSize(), tamLetra = 56) }
            TituloAjustado(j.titulo, tinta, Modifier.weight(1.3f).fillMaxWidth().padding(top = 6.dp))
        }
    }
}
