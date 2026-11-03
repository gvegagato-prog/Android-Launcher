package com.gonzalo.portico

import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
fun ImagenJuego(j: Juego, modifier: Modifier = Modifier) {
    PortadaArco(j, modifier, titulo = false, letra = true, relleno = true)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PantInicio(
    alAbrir: (Juego) -> Unit, alJugar: (Juego) -> Unit, alCambiarImagen: (Juego) -> Unit, alAnadir: (Tipo?) -> Unit,
    alIr: (Destino) -> Unit, alBuscar: () -> Unit
) {
    val ctx = LocalContext.current
    val todos = Biblioteca.juegos.toList()
    val hoy = remember { Calendar.getInstance() }
    val atrio = remember { runCatching { ctx.packageManager.getLaunchIntentForPackage(ATRIO) }.getOrNull() }
    val ongoing = remember(todos) { recientes(todos.filter { it.ultima > 0 }).take(8) }
    val n = ongoing.size
    Column(Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            EnlaceRevista("Buscar", onClick = alBuscar)
            EnlaceRevista("Añadir", Modifier.padding(start = 14.dp), acento = true) { alAnadir(null) }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.Settings, "Ajustes", tint = Charcoal, modifier = Modifier.padding(start = 6.dp).clickable { alIr(Destino.Ajustes) }.padding(8.dp).size(22.dp))
        }
        // Cabecera de revista
        Cabecera(92.sp, Modifier.padding(top = 8.dp))
        FileteCabecera(3.dp)
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text("Nº ${hoy.get(Calendar.DAY_OF_YEAR)}", color = Charcoal, fontFamily = Body, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            Text(SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES")).format(hoy.time).uppercase(), color = Charcoal, fontFamily = Body, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Filete(1.dp)
        Text("Tus juegos, al día", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 15.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp))

        MenuRevista(alIr, alAnadir)

        if (todos.isEmpty()) {
            Spacer(Modifier.height(30.dp))
            Antetitulo("Para empezar", Modifier.padding(bottom = 10.dp))
            Text("Pórtico no es un emulador: es la puerta desde la que abres los que ya tienes, los juegos de Android y los de navegador.", color = Muted, fontFamily = Body, fontSize = 15.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(10.dp))
            MagLink("ROMs y emuladores", "GBA, DS, SNES, PSP… con el emulador que elijas", accent = true) { alAnadir(Tipo.ROM) }
            Filete(1.dp, Line)
            MagLink("Juegos de Android", "Los que ya tienes instalados") { alAnadir(Tipo.APP) }
            Filete(1.dp, Line)
            MagLink("Juegos de navegador", "PokéRogue y otros, a pantalla completa") { alAnadir(Tipo.WEB) }
        }

        // En portada: lo último que has jugado. Se desliza con el dedo y, si no la tocas, pasa sola cada 7 s
        if (n > 0) {
            val vueltas = if (n > 1) n * 1000 else n
            val pager = key(n) { rememberPagerState(initialPage = if (n > 1) n * 500 else 0) { vueltas } }
            LaunchedEffect(pager.settledPage, n) {
                if (n < 2) return@LaunchedEffect
                delay(7_000)
                if (!pager.isScrollInProgress) pager.animateScrollToPage(pager.settledPage + 1, animationSpec = tween(700))
            }
            val actual = pager.currentPage % n
            val scope = rememberCoroutineScope()
            Spacer(Modifier.height(30.dp))
            Column(Modifier.fillMaxWidth().border(1.5.dp, Charcoal).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Antetitulo("En portada", Modifier.padding(bottom = 10.dp))
                    Spacer(Modifier.weight(1f))
                    if (n > 1) Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(n) { i -> Box(Modifier.size(if (i == actual) 8.dp else 6.dp).clip(CircleShape).background(if (i == actual) Acento else Muted.copy(alpha = 0.5f)).clickable { scope.launch { pager.animateScrollToPage(pager.currentPage - actual + i) } }) }
                    }
                }
                HorizontalPager(pager, Modifier.fillMaxWidth(), pageSpacing = 18.dp, verticalAlignment = Alignment.Top, beyondViewportPageCount = (n - 1).coerceIn(0, 6),
                    key = { p -> if (n > 1) p else ongoing.getOrNull(p)?.id ?: p.toString() }) { page ->
                    val j = ongoing[page % n]
                    Column(Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1.35f).clickable { alAbrir(j) }) { ImagenJuego(j, Modifier.matchParentSize()) }
                        Text("CAMBIAR IMAGEN", color = Charcoal, fontFamily = Body, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp, modifier = Modifier.clickable { alCambiarImagen(j) }.padding(top = 6.dp, bottom = 4.dp))
                        val (fs, lh) = headlineSize(j.titulo)
                        FitTitle(j.titulo, fs, lh, letterSpacing = (-1.5).sp, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clickable { alAbrir(j) })
                        Text(metaDe(j), color = Muted, fontFamily = Body, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                        Text("Jugar →", color = Acento, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 10.dp).clickable { alJugar(j) })
                    }
                }
            }
        }

        // Ahora mismo: lo que has jugado hace poco, en lista numerada
        if (n > 1) {
            Spacer(Modifier.height(38.dp))
            Antetitulo("Ahora mismo", Modifier.padding(bottom = 10.dp))
            ongoing.forEachIndexed { i, j ->
                Row(Modifier.fillMaxWidth().clickable { alAbrir(j) }.padding(vertical = 14.dp), verticalAlignment = Alignment.Top) {
                    Text("%02d".format(i + 1), color = Acento, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 30.sp, modifier = Modifier.width(56.dp))
                    Column(Modifier.weight(1f)) {
                        FitTitle(j.titulo, 22.sp, 25.sp, fontWeight = FontWeight.SemiBold)
                        Text(metaDe(j), color = Muted, fontFamily = Body, fontSize = 13.sp)
                    }
                    Text("JUGAR →", color = Acento, fontFamily = Body, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, modifier = Modifier.clickable { alJugar(j) }.padding(start = 8.dp, top = 6.dp))
                }
                Filete(1.dp, Line)
            }
        }

        // Las notas más altas: números gigantes
        val mejores = todos.filter { it.terminado && it.nota > 0f }.sortedByDescending { it.nota }.take(5)
        if (mejores.isNotEmpty()) {
            Spacer(Modifier.height(38.dp))
            Antetitulo("Las notas más altas", Modifier.padding(bottom = 10.dp))
            val numStyle = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 58.sp, lineHeight = 58.sp)
            val medidor = rememberTextMeasurer()
            val d = androidx.compose.ui.platform.LocalDensity.current
            val numW = with(d) { mejores.maxOf { medidor.measure(notaTexto(it.nota), numStyle).size.width }.toDp() } + 16.dp
            mejores.forEach { j ->
                Row(Modifier.fillMaxWidth().clickable { alAbrir(j) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(notaTexto(j.nota), color = Charcoal, style = numStyle, maxLines = 1, softWrap = false, modifier = Modifier.width(maxOf(numW, 100.dp)))
                    Column(Modifier.weight(1f)) {
                        FitTitle(j.titulo, 20.sp, 23.sp, fontWeight = FontWeight.Normal, maxLines = 2)
                        Text(etiquetaDe(j).uppercase(), color = Acento, fontFamily = Body, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    }
                }
            }
        }

        // Lo último que has terminado
        val terminados = todos.filter { it.terminado }.sortedByDescending { it.finalizado }.take(6)
        if (terminados.isNotEmpty()) {
            Spacer(Modifier.height(38.dp))
            Antetitulo("Lo último que has terminado", Modifier.padding(bottom = 6.dp))
            terminados.forEach { j ->
                Row(Modifier.fillMaxWidth().clickable { alAbrir(j) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text((if (j.finalizado > 0) SimpleDateFormat("d MMM yyyy", Locale("es", "ES")).format(Date(j.finalizado)).uppercase() + " · " else "") + etiquetaDe(j).uppercase(), color = Muted, fontFamily = Body, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        FitTitle(j.titulo, 22.sp, 25.sp, fontWeight = FontWeight.Normal, maxLines = 2)
                    }
                    if (j.nota > 0f) Text(notaTexto(j.nota), color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                }
                Filete(1.dp, Line)
            }
        }

        // Sumario
        Spacer(Modifier.height(38.dp))
        Antetitulo("Sumario", Modifier.padding(bottom = 6.dp))
        val roms = todos.count { it.tipo == Tipo.ROM }
        listOf(
            Triple("Juegos", todos.size.toString(), Destino.Juegos), Triple("ROMs", roms.toString(), Destino.Roms), Triple("Android", todos.count { it.tipo == Tipo.APP }.toString(), Destino.Android),
            Triple("Web", todos.count { it.tipo == Tipo.WEB }.toString(), Destino.Web), Triple("Favoritos", todos.count { it.favorito }.toString(), Destino.Favoritos),
            Triple("Sistemas", "→", Destino.Sistemas), Triple("Ajustes", "→", Destino.Ajustes),
        ).forEach { (t, v, d) ->
            Box(Modifier.fillMaxWidth().clickable { alIr(d) }.padding(vertical = 6.dp)) {
                MagFactSerif(t, v)
            }
        }
        if (atrio != null) {
            Spacer(Modifier.height(24.dp))
            Antetitulo("Tu otra app", Modifier.padding(bottom = 6.dp))
            MagLink("Abrir Atrio", "Valora lo que acabas de jugar") { ctx.startActivity(atrio.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
        Spacer(Modifier.height(40.dp))
        Text("Pórtico · hecho en casa", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(24.dp))
    }
}

/** El sumario: nombre en serif, puntos guía y la cifra, como el índice de una revista. */
@Composable
private fun MagFactSerif(t: String, v: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(t, color = Charcoal, fontFamily = Display, fontSize = 20.sp)
        Box(Modifier.weight(1f).height(12.dp).padding(horizontal = 6.dp).drawBehind {
            drawLine(Muted.copy(alpha = 0.6f), Offset(0f, size.height - 2), Offset(size.width, size.height - 2), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 6f)))
        })
        Text(v, color = Acento, fontFamily = Body, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** Las secciones, como el menú de una web: sin título, justo antes de la portada. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MenuRevista(alIr: (Destino) -> Unit, alAnadir: (Tipo?) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Filete(1.dp)
        FlowRow(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            listOf("Juegos" to Destino.Juegos, "ROMs" to Destino.Roms, "Android" to Destino.Android, "Web" to Destino.Web, "Favoritos" to Destino.Favoritos, "Sistemas" to Destino.Sistemas).forEach { (t, d) ->
                Text(t.uppercase(), color = Charcoal, fontFamily = Body, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, maxLines = 1,
                    modifier = Modifier.clickable { alIr(d) }.padding(vertical = 9.dp))
            }
        }
        Filete(1.dp)
    }
}
