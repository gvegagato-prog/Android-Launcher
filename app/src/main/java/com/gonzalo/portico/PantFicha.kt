package com.gonzalo.portico

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Copia la imagen elegida a la carpeta de la app (reducida) y devuelve su ruta: así no depende de permisos. */
suspend fun guardarPortada(c: Context, id: String, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
        var s = 1; while (o.outWidth / s > 1000 || o.outHeight / s > 1400) s *= 2
        val b = c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s }) } ?: return@runCatching null
        val dir = File(c.filesDir, "portadas").apply { mkdirs() }
        val f = File(dir, "$id-${System.currentTimeMillis()}.jpg")
        f.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        f.absolutePath
    }.getOrNull()
}

/** Igual, pero con los bytes de una imagen ya bajada de internet. null = no es una imagen. */
suspend fun guardarPortadaBytes(c: Context, id: String, bytes: ByteArray): String? = withContext(Dispatchers.IO) {
    runCatching {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        if (o.outWidth <= 0) return@runCatching null
        var s = 1; while (o.outWidth / s > 1000 || o.outHeight / s > 1400) s *= 2
        val b = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s }) ?: return@runCatching null
        val f = File(File(c.filesDir, "portadas").apply { mkdirs() }, "$id-${System.currentTimeMillis()}.jpg")
        f.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        f.absolutePath
    }.getOrNull()
}

/** El icono del acceso directo: la portada si hay, o el degradado con la inicial. */
fun iconoAcceso(c: Context, j: Juego): Bitmap {
    val lado = 192
    val bm = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bm)
    val (a, b) = tonosDe(j)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.shader = LinearGradient(0f, 0f, lado.toFloat(), lado.toFloat(), a.toArgb(), b.toArgb(), Shader.TileMode.CLAMP)
    cv.drawRect(0f, 0f, lado.toFloat(), lado.toFloat(), p)
    val cover = j.portada.takeIf { it.isNotBlank() && File(it).exists() }?.let { BitmapFactory.decodeFile(it) }
    if (cover != null) {
        val r = maxOf(lado.toFloat() / cover.width, lado.toFloat() / cover.height)
        val w = cover.width * r; val h = cover.height * r
        cv.drawBitmap(cover, null, android.graphics.RectF((lado - w) / 2, (lado - h) / 2, (lado + w) / 2, (lado + h) / 2), null)
    } else if (j.tipo == Tipo.APP) {
        runCatching { c.packageManager.getApplicationIcon(j.paquete).aBitmap(lado) }.getOrNull()?.let { cv.drawBitmap(it, null, android.graphics.RectF(lado * 0.2f, lado * 0.2f, lado * 0.8f, lado * 0.8f), null) }
    } else {
        val t = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(230, 255, 255, 255); textSize = lado * 0.55f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        cv.drawText(j.titulo.trim().firstOrNull()?.uppercase() ?: "?", lado / 2f, lado * 0.69f, t)
    }
    return bm
}
private fun Color.toArgb(): Int = android.graphics.Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

fun crearAcceso(c: Context, j: Juego): Boolean {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(c)) return false
    val info = ShortcutInfoCompat.Builder(c, "juego_" + j.id).setShortLabel(j.titulo.take(24)).setLongLabel(j.titulo)
        .setIcon(IconCompat.createWithBitmap(iconoAcceso(c, j))).setIntent(LanzarActivity.intent(c, j.id)).build()
    return ShortcutManagerCompat.requestPinShortcut(c, info, null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Ficha(id: String, alCerrar: () -> Unit, alJugar: (Juego) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val j = Biblioteca.por(id)
    if (j == null) { LaunchedEffect(Unit) { alCerrar() }; return }
    var renombrar by remember { mutableStateOf(false) }
    var borrar by remember { mutableStateOf(false) }
    var selector by remember { mutableStateOf(false) }
    var editarWeb by remember { mutableStateOf(false) }
    var eligePortada by remember { mutableStateOf(false) }
    var buscaInternet by remember { mutableStateOf(false) }
    val cambiarPortada = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u ->
        if (u != null) scope.launch { guardarPortada(ctx, j.id, u)?.let { Biblioteca.poner(Biblioteca.por(j.id)!!.copy(portada = it)) } }
    }
    val (a, b) = remember(j.id, j.titulo, j.sistema) { tonosDe(j) }
    val sistema = Catalogo.sistema(j.sistema)

    Box(Modifier.fillMaxSize().background(Paper)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
            // Cabecera: degradado del juego con su arco en grande
            Box(Modifier.fillMaxWidth().height(380.dp).background(Brush.verticalGradient(listOf(a, b, Paper)))) {
                PortadaArco(j, Modifier.align(Alignment.Center).padding(top = 40.dp).width(200.dp), titulo = false)
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    BotonIcono(Icons.Filled.ArrowBack, "Volver", fondo = Color(0x55000000), tinta = Color.White, alHacer = alCerrar)
                    Spacer(Modifier.weight(1f))
                    BotonIcono(if (j.favorito) Icons.Filled.Star else Icons.Filled.StarBorder, if (j.favorito) "Quitar de favoritos" else "Añadir a favoritos", fondo = Color(0x55000000), tinta = if (j.favorito) Color(0xFFFFD24A) else Color.White) {
                        Biblioteca.poner(j.copy(favorito = !j.favorito))
                    }
                }
            }
            Column(Modifier.padding(horizontal = 20.dp).offset(y = (-20).dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Column {
                    Etiqueta(when (j.tipo) { Tipo.ROM -> sistema?.nombre ?: j.sistema; Tipo.APP -> "Juego de Android"; Tipo.WEB -> "Juego de navegador" })
                    Spacer(Modifier.height(4.dp))
                    Titulo(j.titulo, 32, lineas = 3)
                }
                BotonAcento("Jugar", Icons.Filled.PlayArrow, Modifier.fillMaxWidth(), alto = 58.dp) { alJugar(j) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Dato("Veces", j.veces.toString(), Modifier.weight(1f))
                    Dato("Tiempo ≈", tiempoDe(j.msJugados), Modifier.weight(1f))
                    Dato("Última vez", haceCuanto(j.ultima), Modifier.weight(1.2f))
                }
                when (j.tipo) {
                    Tipo.ROM -> {
                        val e = remember(j, selector) { Lanzador.elegir(ctx, j) }
                        Panel(Modifier.fillMaxWidth(), alHacer = { selector = true }) {
                            Etiqueta("Se abre con")
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(e?.etiqueta ?: "Sin emulador para este sistema", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    val r = e?.resultado
                                    Cuerpo(when (r) { is Resultado.Ok -> if (Comando.esRetroArch(e.plantilla)) "Instalado · necesita el núcleo descargado en RetroArch" else "Instalado y listo"; is Resultado.Falta -> r.motivo; null -> "" }, tam = 12, color = if (r is Resultado.Ok) Muted else Acento)
                                }
                                Icon(Icons.Filled.ChevronRight, null, tint = Muted)
                            }
                        }
                    }
                    Tipo.APP -> Panel(Modifier.fillMaxWidth()) { Etiqueta("Paquete"); Cuerpo(j.paquete, color = Charcoal) }
                    Tipo.WEB -> Panel(Modifier.fillMaxWidth(), alHacer = { editarWeb = true }) {
                        Etiqueta("Dirección"); Cuerpo(j.url, color = Charcoal)
                        Spacer(Modifier.height(6.dp)); Cuerpo("Pantalla: " + listOf("automática", "horizontal", "vertical")[j.orientacion.coerceIn(0, 2)], tam = 12)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonSuave("Portada", Icons.Filled.Image, Modifier.weight(1f)) { eligePortada = true }
                        BotonSuave("Renombrar", Icons.Filled.Edit, Modifier.weight(1f)) { renombrar = true }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonSuave("Acceso directo", Icons.Filled.AddToHomeScreen, Modifier.weight(1f)) {
                            if (!crearAcceso(ctx, j)) Lanzador.aviso(ctx, "Tu launcher no deja crear accesos directos")
                        }
                        BotonSuave("Quitar", Icons.Filled.Delete, Modifier.weight(1f), peligro = true) { borrar = true }
                    }
                    if (j.portada.isNotBlank()) Text("Usar la portada automática", color = Acento, fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        modifier = Modifier.clip(pildora()).clickable { Biblioteca.poner(j.copy(portada = "")) }.padding(8.dp))
                }
            }
        }
    }
    if (renombrar) DialogoTexto("Nombre del juego", j.titulo, "Guardar", { renombrar = false }) { t -> if (t.isNotBlank()) Biblioteca.poner(j.copy(titulo = t.trim())); renombrar = false }
    if (borrar) AlertDialog(
        onDismissRequest = { borrar = false }, containerColor = Crudo,
        title = { Text("¿Quitar «${j.titulo}»?", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold) },
        text = { Cuerpo(if (j.tipo == Tipo.ROM) "Solo se quita de Pórtico: el archivo de la ROM no se toca." else "Solo se quita de Pórtico.") },
        confirmButton = { TextButton({ Biblioteca.quitar(j.id); borrar = false; alCerrar() }) { Text("Quitar", color = Color(0xFFE5484D)) } },
        dismissButton = { TextButton({ borrar = false }) { Text("Cancelar", color = Charcoal) } }
    )
    if (eligePortada) AlertDialog(
        onDismissRequest = { eligePortada = false }, containerColor = Crudo,
        title = { Text("Portada de «${j.titulo}»", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonSuave("Buscar en internet", Icons.Filled.Search, Modifier.fillMaxWidth()) { eligePortada = false; buscaInternet = true }
                BotonSuave("Elegir de la galería", Icons.Filled.PhotoLibrary, Modifier.fillMaxWidth()) { eligePortada = false; cambiarPortada.launch("image/*") }
                Cuerpo("Buscar abre Google Imágenes, Pinterest, ArtStation o Newgrounds dentro de Pórtico; tocas una imagen y la usas.", tam = 12)
            }
        },
        confirmButton = {}, dismissButton = { TextButton({ eligePortada = false }) { Text("Cancelar", color = Charcoal) } }
    )
    if (buscaInternet) InternetPicker(
        initialQuery = j.titulo + (if (j.tipo == Tipo.ROM) " " + (sistema?.nombre ?: "") + " portada" else " portada"),
        idJuego = j.id,
        onPick = { ruta -> Biblioteca.por(j.id)?.let { Biblioteca.poner(it.copy(portada = ruta)) }; buscaInternet = false },
        onDismiss = { buscaInternet = false }
    )
    if (selector) SelectorEmulador(j.sistema, j) { selector = false }
    if (editarWeb) DialogoWeb(j) { editarWeb = false }
}

@Composable
private fun Dato(titulo: String, valor: String, modifier: Modifier) {
    Panel(modifier) { Etiqueta(titulo); Spacer(Modifier.height(4.dp)); Text(valor, color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1) }
}

/** Un cuadro con una línea de texto para escribir. */
@Composable
fun DialogoTexto(titulo: String, inicial: String, boton: String, alCerrar: () -> Unit, alAceptar: (String) -> Unit) {
    var t by remember { mutableStateOf(inicial) }
    AlertDialog(
        onDismissRequest = alCerrar, containerColor = Crudo,
        title = { Text(titulo, color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold) },
        text = { Campo(t, "", { t = it }) },
        confirmButton = { TextButton({ alAceptar(t) }) { Text(boton, color = Acento, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(alCerrar) { Text("Cancelar", color = Charcoal) } }
    )
}

@Composable
fun Campo(valor: String, pista: String, alCambiar: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().clip(esquina(14.dp)).background(Sand).padding(horizontal = 14.dp, vertical = 14.dp)) {
        if (valor.isEmpty()) Text(pista, color = Muted, fontFamily = Body, fontSize = 15.sp)
        BasicTextField(valor, alCambiar, singleLine = true, textStyle = TextStyle(color = Charcoal, fontFamily = Body, fontSize = 15.sp), cursorBrush = SolidColor(Acento), modifier = Modifier.fillMaxWidth())
    }
}

/** Qué emulador usar: «Automático» o uno de los del sistema; ✓ si está instalado, «Instalar» si no. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorEmulador(sistemaId: String, juego: Juego?, alCerrar: () -> Unit) {
    val ctx = LocalContext.current
    val sistema = Catalogo.sistema(sistemaId)
    val sonda = remember(juego) { juego ?: Juego(id = "sonda", titulo = "", tipo = Tipo.ROM, sistema = sistemaId, uri = "content://sonda", ruta = "/storage/emulated/0/sonda") }
    val evaluados = remember(sonda) { Lanzador.evaluar(ctx, sonda) }
    var actual by remember { mutableStateOf(if (juego != null) juego.emulador else Ajustes.emuladorPorSistema[sistemaId].orEmpty()) }
    ModalBottomSheet(alCerrar, containerColor = Crudo, contentColor = Charcoal) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Titulo(if (juego != null) "Emulador de este juego" else "Emulador para ${sistema?.nombre ?: sistemaId}", 22)
            Cuerpo("Los que ya tienes instalados salen marcados. «Automático» usa uno propio (sin núcleos que descargar) y, si no hay, RetroArch.", tam = 12)
            Spacer(Modifier.height(8.dp))
            fun elegir(l: String) {
                actual = l
                if (juego != null) Biblioteca.poner(Biblioteca.por(juego.id)!!.copy(emulador = l))
                else { if (l.isEmpty()) Ajustes.emuladorPorSistema.remove(sistemaId) else Ajustes.emuladorPorSistema[sistemaId] = l; Ajustes.guardar(ctx) }
            }
            Opcion("Automático", "Recomendado", actual.isEmpty(), true, null) { elegir("") }
            if (evaluados.isEmpty()) Cuerpo("Pórtico todavía no sabe lanzar este sistema.")
            evaluados.forEach { e ->
                val r = e.resultado
                val ok = r is Resultado.Ok
                val sub = when (r) {
                    is Resultado.Ok -> if (Comando.esRetroArch(e.plantilla)) "Instalado · necesita el núcleo en RetroArch" else "Instalado"
                    is Resultado.Falta -> r.motivo
                }
                Opcion(e.etiqueta, sub, actual == e.etiqueta, ok, (r as? Resultado.Falta)?.paquete) { elegir(e.etiqueta) }
            }
        }
    }
}

@Composable
private fun Opcion(titulo: String, sub: String, elegida: Boolean, instalado: Boolean, paquetePlay: String?, alElegir: () -> Unit) {
    val ctx = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clip(esquina(16.dp)).background(if (elegida) Sand else Color.Transparent).clickable(onClick = alElegir).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (elegida) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked, null, tint = if (elegida) Acento else Muted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, color = Charcoal, fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Cuerpo(sub, tam = 12, color = if (instalado) Muted else Muted.copy(alpha = 0.8f))
        }
        if (!instalado && paquetePlay != null) Text("Instalar", color = Acento, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            modifier = Modifier.clip(pildora()).clickable { Lanzador.tienda(ctx, paquetePlay) }.padding(10.dp))
        else if (instalado && titulo != "Automático") Icon(Icons.Filled.Check, "Instalado", tint = Color(0xFF3DBE8B), modifier = Modifier.size(20.dp))
    }
}
