package com.gonzalo.portico

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun arriba(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 14.dp

/** Estado de un sistema: cuántos juegos tiene y con qué emulador se abren. */
private class EstadoSistema(val s: Sistema, val juegos: Int, val emu: String, val listo: Boolean)

@Composable
fun PantSistemas(alAnadirCarpeta: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val todos = Biblioteca.juegos.toList()
    val conteo = todos.filter { it.tipo == Tipo.ROM }.groupBy { it.sistema }.mapValues { it.value.size }
    var verTodos by remember { mutableStateOf(false) }
    var busca by remember { mutableStateOf("") }
    var abierto by remember { mutableStateOf<String?>(null) }
    var escaneando by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }
    val lista = remember(conteo, verTodos, busca, abierto) {
        Catalogo.sistemas.filter { s -> (verTodos || busca.isNotBlank() || (conteo[s.id] ?: 0) > 0) && (busca.isBlank() || Catalogo.norm(s.nombre).contains(Catalogo.norm(busca)) || s.id == Catalogo.norm(busca)) }
            .map { s ->
                val sonda = Juego(id = "sonda", titulo = "", tipo = Tipo.ROM, sistema = s.id, uri = "content://sonda", ruta = "/storage/emulated/0/sonda")
                val e = Lanzador.elegir(ctx, sonda)
                EstadoSistema(s, conteo[s.id] ?: 0, if (e == null) "Sin comandos" else if (e.resultado is Resultado.Ok) e.etiqueta else "Sin emulador instalado", e?.resultado is Resultado.Ok)
            }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = arriba(), bottom = 130.dp, start = 20.dp, end = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Titulo("Sistemas", 34)
                Cuerpo("Cada consola, con el emulador que tú elijas. Pórtico no incluye emuladores: abre los que ya tienes instalados.", tam = 13)
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Etiqueta("Carpetas de ROMs")
                Spacer(Modifier.height(6.dp))
                if (Biblioteca.carpetas.isEmpty()) Cuerpo("Todavía no has añadido ninguna. Añade la carpeta donde guardas tus ROMs y Pórtico la recorre.", tam = 13)
                else Biblioteca.carpetas.forEach { Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Folder, null, tint = Acento, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(it.nombre, color = Charcoal, fontFamily = Body, fontSize = 14.sp, modifier = Modifier.weight(1f)); Icon(Icons.Filled.Close, "Quitar carpeta", tint = Muted, modifier = Modifier.size(18.dp).clickable { Biblioteca.carpetas.remove(it); Biblioteca.guardar() }) } }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    BotonSuave("Añadir carpeta", Icons.Filled.CreateNewFolder, alHacer = alAnadirCarpeta)
                    if (Biblioteca.carpetas.isNotEmpty()) {
                        if (escaneando) CircularProgressIndicator(Modifier.size(24.dp), color = Acento, strokeWidth = 3.dp)
                        else BotonSuave("Buscar nuevos", Icons.Filled.Refresh) {
                            escaneando = true
                            scope.launch { val i = reescanearCarpetas(ctx); escaneando = false; aviso = if (i.anadidos == 0) "No hay juegos nuevos" else "${i.anadidos} juegos nuevos" }
                        }
                    }
                }
                aviso?.let { Spacer(Modifier.height(6.dp)); Cuerpo(it, tam = 12, color = Acento) }
            }
        }
        item { CampoBusqueda(busca) { busca = it } }
        items(lista, key = { it.s.id }) { e ->
            Row(Modifier.fillMaxWidth().clip(esquina(20.dp)).background(Crudo).clickable { abierto = e.s.id }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp, 58.dp).clip(arco()).background(e.s.color), contentAlignment = Alignment.Center) {
                    Text(e.s.corto, color = Color.White, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = if (e.s.corto.length > 3) 12.sp else 15.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.s.nombre, color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                    Cuerpo("${if (e.juegos == 0) "Sin juegos" else if (e.juegos == 1) "1 juego" else "${e.juegos} juegos"} · ${e.emu}", tam = 12, lineas = 1)
                }
                Icon(if (e.listo) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline, if (e.listo) "Emulador listo" else "Falta emulador", tint = if (e.listo) Color(0xFF3DBE8B) else Muted, modifier = Modifier.size(22.dp))
            }
        }
        item {
            if (busca.isBlank()) Text(if (verTodos) "Ver solo los míos" else "Ver los ${Catalogo.sistemas.size} sistemas", color = Acento, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.clip(pildora()).clickable { verTodos = !verTodos }.padding(10.dp))
        }
    }
    abierto?.let { SelectorEmulador(it, null) { abierto = null } }
}
