package com.gonzalo.portico

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun saludo(h: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) = when { h in 6..13 -> "Buenos días"; h in 14..20 -> "Buenas tardes"; else -> "Buenas noches" }

/** Ordena los juegos: los últimos que jugaste primero y, si no, los últimos que añadiste. */
fun recientes(l: List<Juego>) = l.sortedWith(compareByDescending<Juego> { it.ultima }.thenByDescending { it.anadido })

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PantInicio(alAbrir: (Juego) -> Unit, alJugar: (Juego) -> Unit, alAnadir: (Tipo) -> Unit, alVerSistema: (String?, Tipo?) -> Unit, alEstilo: () -> Unit) {
    val ctx = LocalContext.current
    val todos = Biblioteca.juegos.toList()
    val fecha = remember { SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES")).format(Date()) }
    val atrio = remember { runCatching { ctx.packageManager.getLaunchIntentForPackage(ATRIO) }.getOrNull() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = arriba(), bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Etiqueta(fecha)
                    Spacer(Modifier.height(4.dp))
                    Titulo(saludo(), 34)
                }
                BotonIcono(Icons.Filled.Tune, "Estilo", alHacer = alEstilo)
            }
        }
        if (todos.isEmpty()) {
            item { Bienvenida(alAnadir) }
        } else {
            val ultimos = recientes(todos).take(5)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Seccion(if (todos.any { it.ultima > 0 }) "Sigue donde lo dejaste" else "Para empezar")
                    val pager = rememberPagerState { ultimos.size }
                    HorizontalPager(pager, contentPadding = PaddingValues(horizontal = 20.dp), pageSpacing = 12.dp) { i -> Heroe(ultimos[i], alAbrir, alJugar) }
                    if (ultimos.size > 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        repeat(ultimos.size) { i -> Box(Modifier.padding(3.dp).size(if (i == pager.currentPage) 18.dp else 6.dp, 6.dp).clip(CircleShape).background(if (i == pager.currentPage) Charcoal else Line)) }
                    }
                }
            }
            val favs = todos.filter { it.favorito }
            if (favs.isNotEmpty()) item { FilaJuegos("Favoritos", null, favs, alAbrir, alJugar) { alVerSistema(null, null) } }
            val nuevos = todos.sortedByDescending { it.anadido }.take(12)
            item { FilaJuegos("Añadidos hace poco", null, nuevos, alAbrir, alJugar, null) }
            // Una estantería por sistema
            val porSistema = todos.filter { it.tipo == Tipo.ROM }.groupBy { it.sistema }.entries.sortedByDescending { it.value.size }
            items(porSistema.toList(), key = { it.key }) { (id, js) ->
                val s = Catalogo.sistema(id)
                FilaJuegos(s?.nombre ?: id, "${js.size} ${if (js.size == 1) "juego" else "juegos"}", js.sortedBy { it.titulo.lowercase() }, alAbrir, alJugar) { alVerSistema(id, null) }
            }
            val apps = todos.filter { it.tipo == Tipo.APP }
            if (apps.isNotEmpty()) item { FilaJuegos("Juegos de Android", "${apps.size}", apps, alAbrir, alJugar) { alVerSistema(null, Tipo.APP) } }
            val webs = todos.filter { it.tipo == Tipo.WEB }
            if (webs.isNotEmpty()) item { FilaJuegos("Juegos web", "${webs.size}", webs, alAbrir, alJugar) { alVerSistema(null, Tipo.WEB) } }
        }
        if (atrio != null) item {
            Panel(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), alHacer = { ctx.startActivity(atrio.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MenuBook, null, tint = Acento, modifier = Modifier.size(26.dp)); Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Etiqueta("Tu otra app")
                        Text("Abrir Atrio", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Cuerpo("Valora lo que acabas de jugar y apúntalo en tu diario.", tam = 12)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = Muted)
                }
            }
        }
    }
}

const val ATRIO = "com.gonzalo.gamerate"

/** Una tarjeta grande: la portada en arco, el título y «Jugar». */
@Composable
fun Heroe(j: Juego, alAbrir: (Juego) -> Unit, alJugar: (Juego) -> Unit) {
    val (a, b) = remember(j.id, j.titulo, j.sistema) { tonosDe(j) }
    val forma = esquina(32.dp)
    Row(
        Modifier.fillMaxWidth().height(210.dp).clip(forma).background(Brush.linearGradient(listOf(a.copy(alpha = 0.95f), b))).clickable { alAbrir(j) }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PortadaArco(j, Modifier.fillMaxHeight().aspectRatio(0.75f), titulo = false)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Etiqueta(etiquetaDe(j) + if (j.ultima > 0) " · " + haceCuanto(j.ultima) else "", color = Color.White.copy(alpha = 0.75f))
            Spacer(Modifier.height(4.dp))
            Text(j.titulo, color = Color.White, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 24.sp, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.height(44.dp).clip(pildora()).background(Color.White).clickable { alJugar(j) }.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFF14151A), modifier = Modifier.size(22.dp)); Spacer(Modifier.width(6.dp))
                Text("Jugar", color = Color(0xFF14151A), fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun FilaJuegos(titulo: String, detalle: String?, juegos: List<Juego>, alAbrir: (Juego) -> Unit, alJugar: (Juego) -> Unit, alVerTodo: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Seccion(titulo, detalle, alVerTodo)
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(juegos, key = { it.id }) { j ->
                PortadaArco(j, Modifier.width(124.dp).clickable { alAbrir(j) }, alJugar = { alJugar(j) })
            }
        }
    }
}

/** Biblioteca vacía: tres puertas para empezar. */
@Composable
fun Bienvenida(alAnadir: (Tipo) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Titulo("Todos tus juegos, una sola puerta", 22)
        Cuerpo("Pórtico no es un emulador: es el sitio desde donde abres los que ya tienes. Elige por dónde empezar.")
        Puerta(Icons.Filled.Memory, "ROMs y emuladores", "GBA, DS, SNES, PSP… con el emulador que tú elijas", Color(0xFF6C4DD6), Color(0xFF2A1A66)) { alAnadir(Tipo.ROM) }
        Puerta(Icons.Filled.Android, "Juegos de Android", "Los que ya tienes instalados en el móvil", Color(0xFF1F9D72), Color(0xFF0B3D2E)) { alAnadir(Tipo.APP) }
        Puerta(Icons.Filled.Language, "Juegos de navegador", "PokéRogue y otros: pantalla completa, sin barra", Color(0xFFE85A2E), Color(0xFF5A1D0A)) { alAnadir(Tipo.WEB) }
    }
}

@Composable
private fun Puerta(icono: ImageVector, titulo: String, detalle: String, a: Color, b: Color, alHacer: () -> Unit) {
    val forma = arco()
    Row(
        Modifier.fillMaxWidth().height(104.dp).clip(esquina(28.dp)).background(Brush.linearGradient(listOf(a, b))).clickable(onClick = alHacer).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.fillMaxHeight().aspectRatio(0.8f).clip(forma).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(icono, null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, color = Color.White, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(detalle, color = Color.White.copy(alpha = 0.78f), fontFamily = Body, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Icon(Icons.Filled.Add, null, tint = Color.White)
    }
}
