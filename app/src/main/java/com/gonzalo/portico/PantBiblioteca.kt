package com.gonzalo.portico

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Orden(val nombre: String) { Recientes("Recientes"), Nombre("Nombre"), Jugados("Más jugados"), Anadidos("Añadidos"), Nota("Nota") }

/** Los filtros de la biblioteca (viven en la raíz: el menú de la portada los pone). */
class FiltroBib {
    var tipo by mutableStateOf<Tipo?>(null)
    var sistema by mutableStateOf<String?>(null)
    var favoritos by mutableStateOf(false)
    var busca by mutableStateOf("")
    var orden by mutableStateOf(Orden.Recientes)
    var buscando by mutableStateOf(false)
    fun limpiar() { tipo = null; sistema = null; favoritos = false; busca = "" }
}

fun filtrar(todos: List<Juego>, f: FiltroBib): List<Juego> {
    val q = Catalogo.norm(f.busca)
    val l = todos.filter { j ->
        (f.tipo == null || j.tipo == f.tipo) && (f.sistema == null || j.sistema == f.sistema) && (!f.favoritos || j.favorito) &&
            (q.isEmpty() || Catalogo.norm(j.titulo).contains(q) || Catalogo.norm(etiquetaDe(j)).contains(q))
    }
    return when (f.orden) {
        Orden.Recientes -> recientes(l)
        Orden.Nombre -> l.sortedBy { it.titulo.lowercase() }
        Orden.Jugados -> l.sortedWith(compareByDescending<Juego> { it.msJugados }.thenByDescending { it.veces })
        Orden.Anadidos -> l.sortedByDescending { it.anadido }
        Orden.Nota -> l.sortedByDescending { it.nota }
    }
}

/** La sección «Juegos»: título enorme, pestañas, filtros y una lista de texto con su «Jugar →». */
@Composable
fun PantBiblioteca(f: FiltroBib, alVolver: () -> Unit, alAbrir: (Juego) -> Unit, alJugar: (Juego) -> Unit, alAnadir: () -> Unit) {
    val todos = Biblioteca.juegos.toList()
    val lista = filtrar(todos, f)
    val sistemas = todos.filter { it.tipo == Tipo.ROM }.groupBy { it.sistema }.entries.sortedByDescending { it.value.size }
    val pestanas = listOf("Todo", "ROMs", "Android", "Web", "Favoritos")
    val sel = when { f.favoritos -> 4; f.tipo == Tipo.ROM -> 1; f.tipo == Tipo.APP -> 2; f.tipo == Tipo.WEB -> 3; else -> 0 }
    val cuentas = listOf(todos.size, todos.count { it.tipo == Tipo.ROM }, todos.count { it.tipo == Tipo.APP }, todos.count { it.tipo == Tipo.WEB }, todos.count { it.favorito })
    val foco = remember { FocusRequester() }
    LaunchedEffect(f.buscando) { if (f.buscando) runCatching { foco.requestFocus() } }
    LazyColumn(Modifier.fillMaxSize().background(Paper).statusBarsPadding(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp)) {
        item {
            Column {
                RevistaTopBar(alVolver, "Pórtico") {
                    EnlaceRevista("Buscar") { f.buscando = !f.buscando; if (!f.buscando) f.busca = "" }
                    EnlaceRevista("+ Añadir", acento = true, onClick = alAnadir)
                }
                Antetitulo("Sección · ${todos.size} ${if (todos.size == 1) "ficha" else "fichas"}", Modifier.padding(top = 10.dp), color = Acento, conCuadro = false)
                FitTitle("Juegos", 52.sp, 56.sp, letterSpacing = (-1.5).sp, modifier = Modifier.padding(top = 2.dp), unaLinea = true)
                Filete(3.dp)
                PestanasRevista(pestanas, sel, cuentas = cuentas) { i ->
                    f.sistema = null; f.favoritos = i == 4
                    f.tipo = when (i) { 1 -> Tipo.ROM; 2 -> Tipo.APP; 3 -> Tipo.WEB; else -> null }
                }
                // Orden y sistemas
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FiltrosRevista(Orden.entries.map { it.nombre }, f.orden.ordinal, Modifier.weight(1f)) { f.orden = Orden.entries[it] }
                }
                if (sistemas.size > 1 && (f.tipo == null || f.tipo == Tipo.ROM)) Row(Modifier.fillMaxWidth()) {
                    FiltrosRevista(listOf("Todos") + sistemas.map { (Catalogo.sistema(it.key)?.corto ?: it.key) + " " + it.value.size }, sistemas.indexOfFirst { it.key == f.sistema } + 1) { i ->
                        if (i == 0) f.sistema = null else { f.sistema = sistemas[i - 1].key; f.tipo = Tipo.ROM }
                    }
                }
                if (f.buscando) CampoBusquedaRevista(f.busca, "Buscar en juegos", foco) { f.busca = it }
                Filete(1.dp, Line)
            }
        }
        if (lista.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(top = 36.dp)) {
                Text(if (todos.isEmpty()) "Todavía no hay nada aquí." else "Nada con ese filtro.", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 18.sp)
                if (todos.isEmpty()) MagLink("Añadir juegos", accent = true, onClick = alAnadir)
            }
        }
        items(lista, key = { it.id }) { j ->
            FilaRevista(j.titulo, listOf(etiquetaDe(j), if (j.msJugados >= 60_000) tiempoDe(j.msJugados) else "", if (j.terminado) "terminado" else "", if (j.enOtroDispositivo) "en otro dispositivo" else "").filter { it.isNotBlank() }.joinToString(" · "),
                derecha = {
                    if (j.terminado && j.nota > 0f) Text(notaTexto(j.nota), color = Muted, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 34.sp, modifier = Modifier.padding(start = 10.dp))
                    Text("JUGAR →", color = Acento, fontFamily = Body, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp,
                        modifier = Modifier.clickable { alJugar(j) }.padding(start = 12.dp, top = 12.dp, bottom = 12.dp))
                }) { alAbrir(j) }
        }
    }
}

/** La casilla de búsqueda de la revista: «Buscar» en versalitas y el texto en cursiva sobre un filete. */
@Composable
fun CampoBusquedaRevista(valor: String, pista: String, foco: FocusRequester? = null, alCambiar: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Bottom) {
            Text("BUSCAR", color = Muted, fontFamily = Body, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(end = 10.dp, bottom = 3.dp))
            Box(Modifier.weight(1f)) {
                if (valor.isEmpty()) Text(pista, color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 18.sp)
                BasicTextField(valor, alCambiar, singleLine = true, textStyle = TextStyle(color = Charcoal, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 18.sp), cursorBrush = SolidColor(Acento),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), modifier = Modifier.fillMaxWidth().let { if (foco != null) it.focusRequester(foco) else it })
            }
            if (valor.isNotEmpty()) Text("✕", color = Muted, fontSize = 16.sp, modifier = Modifier.clickable { alCambiar("") }.padding(start = 8.dp))
        }
        Filete(2.dp)
    }
}

/** Un campo de búsqueda suelto (para los diálogos): el mismo, con otro nombre. */
@Composable
fun CampoBusqueda(valor: String, alCambiar: (String) -> Unit) = CampoBusquedaRevista(valor, "Buscar", null, alCambiar)
