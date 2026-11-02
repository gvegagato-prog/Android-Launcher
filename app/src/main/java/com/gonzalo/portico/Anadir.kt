package com.gonzalo.portico

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Una app instalada que se puede lanzar. */
class AppInstalada(val paquete: String, val nombre: String, val esJuego: Boolean)

@Suppress("DEPRECATION")
suspend fun appsLanzables(c: Context): List<AppInstalada> = withContext(Dispatchers.IO) {
    val pm = c.packageManager
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    pm.queryIntentActivities(i, 0).mapNotNull { r ->
        val a = r.activityInfo?.applicationInfo ?: return@mapNotNull null
        if (a.packageName == c.packageName) return@mapNotNull null
        val juego = a.category == ApplicationInfo.CATEGORY_GAME || (a.flags and ApplicationInfo.FLAG_IS_GAME) != 0
        AppInstalada(a.packageName, r.loadLabel(pm).toString(), juego)
    }.distinctBy { it.paquete }.sortedWith(compareByDescending<AppInstalada> { it.esJuego }.thenBy { it.nombre.lowercase() })
}

/** Vuelve a mirar las carpetas de ROMs que diste y añade lo nuevo. */
suspend fun reescanearCarpetas(c: Context): Informe {
    var a = 0; var r = 0; val sin = ArrayList<Hallazgo>()
    for (f in Biblioteca.carpetas.toList()) {
        val h = runCatching { Escaneo.listar(c, Uri.parse(f.uri)) }.getOrDefault(emptyList())
        val inf = Escaneo.importar(h, externos = android.os.Environment.getExternalStorageDirectory().absolutePath)
        a += inf.anadidos; r += inf.repetidos; sin += inf.sinSistema
    }
    return Informe(a, r, sin)
}

private sealed interface Paso {
    object Menu : Paso
    object Apps : Paso
    class Preguntar(val pendientes: List<Hallazgo>, val anadidosAntes: Int) : Paso
    class Trabajando(val texto: String) : Paso
    class Resumen(val texto: String) : Paso
}

/** «Añadir»: ROMs sueltas, una carpeta de ROMs, juegos de Android o un juego web. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaAnadir(inicial: Tipo?, alCerrar: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var paso by remember { mutableStateOf<Paso>(if (inicial == Tipo.APP) Paso.Apps else Paso.Menu) }
    var web by remember { mutableStateOf(inicial == Tipo.WEB) }
    val externos = remember { android.os.Environment.getExternalStorageDirectory().absolutePath }

    fun terminar(inf: Informe, antes: Int) {
        val total = antes + inf.anadidos
        if (inf.sinSistema.isNotEmpty()) paso = Paso.Preguntar(inf.sinSistema, total)
        else paso = Paso.Resumen(resumen(total, inf.repetidos, 0))
    }

    val archivos = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        paso = Paso.Trabajando("Mirando ${uris.size} archivos…")
        scope.launch {
            val hs = withContext(Dispatchers.IO) { uris.mapNotNull { u -> Escaneo.recordar(ctx, u); Escaneo.hallazgoSuelto(ctx, u) } }
            terminar(Escaneo.importar(hs, externos = externos), 0)
        }
    }
    val carpeta = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { u ->
        if (u == null) return@rememberLauncherForActivityResult
        Escaneo.recordar(ctx, u)
        val nombre = runCatching { android.provider.DocumentsContract.getTreeDocumentId(u).substringAfterLast('/').substringAfter(':').ifEmpty { "Carpeta" } }.getOrDefault("Carpeta")
        if (Biblioteca.carpetas.none { it.uri == u.toString() }) { Biblioteca.carpetas.add(Carpeta(u.toString(), nombre)); Biblioteca.guardar() }
        paso = Paso.Trabajando("Buscando juegos en «$nombre»…")
        scope.launch {
            val hs = Escaneo.listar(ctx, u) { n -> paso = Paso.Trabajando("Buscando juegos en «$nombre»… $n") }
            terminar(Escaneo.importar(hs, externos = externos), 0)
        }
    }

    ModalBottomSheet(alCerrar, containerColor = Crudo, contentColor = Charcoal) {
        when (val p = paso) {
            Paso.Menu -> Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Titulo("Añadir juegos", 24)
                Cuerpo("Elige de dónde vienen.", tam = 13)
                Spacer(Modifier.height(6.dp))
                Entrada(Icons.Filled.FolderOpen, "Carpeta de ROMs", "Pórtico busca dentro y reconoce el sistema por la extensión o por el nombre de la carpeta") { carpeta.launch(null) }
                Entrada(Icons.Filled.InsertDriveFile, "ROMs sueltas", "Elige uno o varios archivos (.gba, .nds, .sfc…)") { archivos.launch(arrayOf("*/*")) }
                Entrada(Icons.Filled.Android, "Juego de Android", "De los que ya tienes instalados") { paso = Paso.Apps }
                Entrada(Icons.Filled.Language, "Juego de navegador", "Una dirección web: PokéRogue y similares") { web = true }
            }
            Paso.Apps -> SelectorApps({ elegidas ->
                val ahora = System.currentTimeMillis(); var n = 0
                elegidas.forEach { a -> val j = Juego(Biblioteca.nuevoId(), a.nombre, Tipo.APP, paquete = a.paquete, anadido = ahora - n); if (!Biblioteca.repetido(j)) { Biblioteca.juegos.add(j); n++ } }
                Biblioteca.guardar(); paso = Paso.Resumen(resumen(n, elegidas.size - n, 0))
            })
            is Paso.Preguntar -> PreguntaSistema(p.pendientes.size) { s ->
                val inf = Escaneo.importar(p.pendientes, forzar = s, externos = externos)
                paso = Paso.Resumen(resumen(p.anadidosAntes + inf.anadidos, inf.repetidos, 0))
            }
            is Paso.Trabajando -> Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CircularProgressIndicator(color = Acento); Cuerpo(p.texto)
            }
            is Paso.Resumen -> Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Filled.CheckCircle, null, tint = Acento, modifier = Modifier.size(44.dp))
                Titulo(p.texto, 20, lineas = 4)
                BotonAcento("Listo", alHacer = alCerrar)
            }
        }
    }
    if (web) DialogoWeb(null) { web = false; if (paso == Paso.Menu) alCerrar() }
}

private fun resumen(a: Int, rep: Int, sin: Int): String =
    (if (a == 0) "No se ha añadido nada nuevo" else if (a == 1) "Se ha añadido 1 juego" else "Se han añadido $a juegos") + (if (rep > 0) " ($rep ya estaban)" else "")

@Composable
private fun Entrada(icono: ImageVector, titulo: String, detalle: String, alHacer: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(esquina(18.dp)).clickable(onClick = alHacer).padding(vertical = 12.dp, horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(esquina(14.dp)).background(Sand), contentAlignment = Alignment.Center) { Icon(icono, null, tint = Acento, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Cuerpo(detalle, tam = 12)
        }
    }
}

@Composable
private fun PreguntaSistema(n: Int, alElegir: (Sistema?) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Titulo("¿De qué consola son?", 22)
        Cuerpo("$n ${if (n == 1) "archivo" else "archivos"} (.zip, .bin, .iso…) valen para varias. Elige el sistema para ${if (n == 1) "él" else "todos"}; si no, se dejan fuera.", tam = 13)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) { items(Catalogo.deCasa, key = { it.id }) { s -> Chip(s.nombre, false) { alElegir(s) } } }
        Text("Dejarlos fuera", color = Acento, fontFamily = Body, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(pildora()).clickable { alElegir(null) }.padding(8.dp))
    }
}

@Composable
private fun SelectorApps(alAnadir: (List<AppInstalada>) -> Unit) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppInstalada>?>(null) }
    var busca by remember { mutableStateOf("") }
    var soloJuegos by remember { mutableStateOf(true) }
    val marcadas = remember { mutableStateListOf<String>() }
    LaunchedEffect(Unit) { apps = appsLanzables(ctx) }
    val yaEstan = Biblioteca.juegos.filter { it.tipo == Tipo.APP }.map { it.paquete }.toSet()
    val lista = apps.orEmpty().filter { (!soloJuegos || it.esJuego) && it.paquete !in yaEstan && (busca.isBlank() || Catalogo.norm(it.nombre).contains(Catalogo.norm(busca))) }
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.88f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Titulo("Juegos de Android", 22)
        CampoBusqueda(busca) { busca = it }
        Row { Chip("Solo juegos", soloJuegos) { soloJuegos = true }; Chip("Todas las apps", !soloJuegos) { soloJuegos = false } }
        if (apps == null) Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Acento) }
        else if (lista.isEmpty()) Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Cuerpo(if (soloJuegos) "No encuentro juegos. Prueba «Todas las apps»: Android solo marca como juego lo que la propia app declara." else "Nada que añadir.")
        }
        else LazyColumn(Modifier.weight(1f)) {
            items(lista, key = { it.paquete }) { a ->
                val on = a.paquete in marcadas
                Row(Modifier.fillMaxWidth().clip(esquina(14.dp)).clickable { if (on) marcadas.remove(a.paquete) else marcadas.add(a.paquete) }.padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    rememberIconoApp(a.paquete)?.let { Image(it, null, Modifier.size(42.dp).clip(esquina(10.dp))) } ?: Box(Modifier.size(42.dp).background(Sand))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(a.nombre, color = Charcoal, fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1); Cuerpo(a.paquete, tam = 11, lineas = 1) }
                    Icon(if (on) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank, null, tint = if (on) Acento else Muted)
                }
            }
        }
        BotonAcento(if (marcadas.isEmpty()) "Elige alguno" else "Añadir ${marcadas.size}", Icons.Filled.Add, Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            if (marcadas.isNotEmpty()) alAnadir(apps.orEmpty().filter { it.paquete in marcadas })
        }
    }
}

/** Direcciones de ejemplo (comprobadas: responden). */
private val SUGERENCIAS = listOf("PokéRogue" to "https://pokerogue.net")

/** Normaliza lo que escribió el usuario: añade https:// si falta y solo admite http(s). null = no vale. */
fun normalizarUrl(t: String): String? {
    val s = t.trim().let { if (it.isEmpty()) return null else if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(it)) it else "https://$it" }
    val u = runCatching { Uri.parse(s) }.getOrNull() ?: return null
    if (u.scheme != "https" && u.scheme != "http") return null
    return if (u.host.isNullOrBlank() || !u.host!!.contains('.')) null else s
}

/** Crear o editar un juego de navegador. */
@Composable
fun DialogoWeb(juego: Juego?, alCerrar: () -> Unit) {
    var url by remember { mutableStateOf(juego?.url ?: "") }
    var nombre by remember { mutableStateOf(juego?.titulo ?: "") }
    var orient by remember { mutableStateOf(juego?.orientacion ?: 0) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = alCerrar, containerColor = Crudo,
        title = { Text(if (juego == null) "Juego de navegador" else "Editar", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (juego == null) LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) { items(SUGERENCIAS) { (n, u) -> Chip(n, false) { url = u; nombre = n } } }
                Campo(url, "Dirección (https://…)", { url = it; error = null })
                Campo(nombre, "Nombre", { nombre = it })
                Etiqueta("Pantalla")
                Row { listOf("Automática", "Horizontal", "Vertical").forEachIndexed { i, t -> Chip(t, orient == i) { orient = i } } }
                error?.let { Text(it, color = Color(0xFFE5484D), fontFamily = Body, fontSize = 13.sp) }
                Cuerpo("Se abre a pantalla completa y guarda sus cookies y datos (ahí viven las partidas de la mayoría de juegos web).", tam = 12)
            }
        },
        confirmButton = {
            TextButton({
                val u = normalizarUrl(url)
                if (u == null) { error = "Esa dirección no vale. Ejemplo: pokerogue.net"; return@TextButton }
                val titulo = nombre.trim().ifEmpty { Uri.parse(u).host!!.removePrefix("www.") }
                if (juego == null) { val j = Juego(Biblioteca.nuevoId(), titulo, Tipo.WEB, url = u, anadido = System.currentTimeMillis(), orientacion = orient); if (!Biblioteca.repetido(j)) Biblioteca.poner(j) }
                else Biblioteca.poner(juego.copy(titulo = titulo, url = u, orientacion = orient))
                alCerrar()
            }) { Text(if (juego == null) "Añadir" else "Guardar", color = Acento, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(alCerrar) { Text("Cancelar", color = Charcoal) } }
    )
}
