package com.gonzalo.portico

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Orden(val nombre: String) { Recientes("Recientes"), Nombre("Nombre"), Jugados("Más jugados"), Anadidos("Añadidos") }

/** Los filtros de la biblioteca (viven en la raíz: «Ver todo» desde Inicio los pone). */
class FiltroBib {
    var tipo by mutableStateOf<Tipo?>(null)
    var sistema by mutableStateOf<String?>(null)
    var favoritos by mutableStateOf(false)
    var busca by mutableStateOf("")
    var orden by mutableStateOf(Orden.Recientes)
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
    }
}

@Composable
fun PantBiblioteca(f: FiltroBib, alAbrir: (Juego) -> Unit, alJugar: (Juego) -> Unit, alAnadir: () -> Unit) {
    val todos = Biblioteca.juegos.toList()
    val lista = filtrar(todos, f)
    var buscando by remember { mutableStateOf(f.busca.isNotEmpty()) }
    var menuOrden by remember { mutableStateOf(false) }
    val sistemas = todos.filter { it.tipo == Tipo.ROM }.groupBy { it.sistema }.entries.sortedByDescending { it.value.size }
    val cols = when (Ajustes.tamano) { 1 -> 2; 2 -> 4; else -> 3 }
    LazyVerticalGrid(
        GridCells.Fixed(cols), Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = arriba(), bottom = 130.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(cols) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Titulo("Juegos", 34, Modifier.weight(1f))
                    BotonIcono(Icons.Filled.Search, "Buscar") { buscando = !buscando; if (!buscando) f.busca = "" }
                    Spacer(Modifier.width(8.dp))
                    Box {
                        BotonIcono(Icons.Filled.SwapVert, "Ordenar") { menuOrden = true }
                        androidx.compose.material3.DropdownMenu(menuOrden, { menuOrden = false }, containerColor = Crudo) {
                            Orden.entries.forEach { o ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(o.nombre, color = if (o == f.orden) Acento else Charcoal, fontFamily = Body, fontWeight = if (o == f.orden) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = { f.orden = o; menuOrden = false })
                            }
                        }
                    }
                }
                if (buscando) CampoBusqueda(f.busca) { f.busca = it }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    item { Chip("Todo", f.tipo == null && !f.favoritos && f.sistema == null, cuenta = todos.size) { f.limpiar() } }
                    item { Chip("ROMs", f.tipo == Tipo.ROM && f.sistema == null, cuenta = todos.count { it.tipo == Tipo.ROM }) { f.tipo = Tipo.ROM; f.sistema = null; f.favoritos = false } }
                    item { Chip("Android", f.tipo == Tipo.APP, cuenta = todos.count { it.tipo == Tipo.APP }) { f.tipo = Tipo.APP; f.sistema = null; f.favoritos = false } }
                    item { Chip("Web", f.tipo == Tipo.WEB, cuenta = todos.count { it.tipo == Tipo.WEB }) { f.tipo = Tipo.WEB; f.sistema = null; f.favoritos = false } }
                    item { Chip("Favoritos", f.favoritos, cuenta = todos.count { it.favorito }) { f.favoritos = !f.favoritos } }
                }
                if (sistemas.size > 1 && (f.tipo == null || f.tipo == Tipo.ROM)) LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(sistemas, key = { it.key }) { (id, js) ->
                        val s = Catalogo.sistema(id)
                        Chip(s?.corto ?: id, f.sistema == id, cuenta = js.size) { if (f.sistema == id) f.sistema = null else { f.sistema = id; f.tipo = Tipo.ROM } }
                    }
                }
            }
        }
        if (lista.isEmpty()) item(span = { GridItemSpan(cols) }) {
            Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.VideogameAsset, null, tint = Muted, modifier = Modifier.size(44.dp))
                Cuerpo(if (todos.isEmpty()) "Todavía no hay nada aquí." else "Nada con ese filtro.")
                if (todos.isEmpty()) BotonAcento("Añadir juegos", Icons.Filled.Add, alHacer = alAnadir)
            }
        }
        items(lista, key = { it.id }) { j ->
            Column(Modifier.clickable { alAbrir(j) }) {
                PortadaArco(j, Modifier.fillMaxWidth(), alJugar = { alJugar(j) })
            }
        }
    }
}

@Composable
fun CampoBusqueda(valor: String, alCambiar: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).clip(pildora()).background(Sand).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Search, null, tint = Muted, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (valor.isEmpty()) Text("Buscar un juego", color = Muted, fontFamily = Body, fontSize = 15.sp)
            BasicTextField(valor, alCambiar, singleLine = true, textStyle = TextStyle(color = Charcoal, fontFamily = Body, fontSize = 15.sp), cursorBrush = SolidColor(Acento), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), modifier = Modifier.fillMaxWidth())
        }
        if (valor.isNotEmpty()) Icon(Icons.Filled.Close, "Borrar", tint = Muted, modifier = Modifier.size(20.dp).clickable { alCambiar("") })
    }
}
