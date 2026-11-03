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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontStyle
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun Ficha(id: String, alCerrar: () -> Unit, alJugar: (Juego) -> Unit, abrirPortada: Boolean = false, alVerPortada: () -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val j = Biblioteca.por(id)
    if (j == null) { LaunchedEffect(Unit) { alCerrar() }; return }
    var renombrar by remember { mutableStateOf(false) }
    var borrar by remember { mutableStateOf(false) }
    var selector by remember { mutableStateOf(false) }
    var editarWeb by remember { mutableStateOf(false) }
    var eligePortada by remember { mutableStateOf(abrirPortada) }
    var disenando by remember { mutableStateOf(false) }
    var editarHoras by remember { mutableStateOf(false) }
    var buscaInternet by remember { mutableStateOf(false) }
    LaunchedEffect(abrirPortada) { if (abrirPortada) alVerPortada() }
    val cambiarPortada = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u ->
        if (u != null) scope.launch { guardarPortada(ctx, j.id, u)?.let { Biblioteca.poner(Biblioteca.por(j.id)!!.copy(portada = it)) } }
    }
    val sistema = Catalogo.sistema(j.sistema)

    Column(Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
        RevistaTopBar(alCerrar, "Volver") {
            if (!disenando) EnlaceRevista(if (j.favorito) "★ Favorito" else "☆ Favorito", acento = j.favorito) { Biblioteca.poner(j.copy(favorito = !j.favorito)) }
            EnlaceRevista(if (disenando) "Listo" else "Editar", acento = disenando) { disenando = !disenando }
        }
        Filete(3.dp)
        Spacer(Modifier.height(14.dp))
        RevistaHeadline(j.titulo, when (j.tipo) { Tipo.ROM -> listOfNotNull(sistema?.corto, sistema?.nombre).joinToString(" · "); Tipo.APP -> "Juego de Android"; Tipo.WEB -> "Juego de navegador" })
        val deck = listOfNotNull(
            if (j.terminado) (if (j.finalizado > 0) "terminado el " + fechaCorta(j.finalizado) else "terminado") else null,
            if (j.msJugados >= 60_000) tiempoDe(j.msJugados) else null
        ).joinToString(" · ")
        if (deck.isNotBlank()) Text(deck, color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 17.sp, modifier = Modifier.padding(vertical = 6.dp))
        Spacer(Modifier.height(10.dp))
        // Las acciones de la ficha: una fila de enlaces entre dos filetes
        Filete(1.dp)
        androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            EnlaceRevista("Jugar\u00A0→", acento = true) { alJugar(j) }
            EnlaceRevista("Portada\u00A0→") { eligePortada = true }
            EnlaceRevista("Acceso directo\u00A0→") { if (!crearAcceso(ctx, j)) Lanzador.aviso(ctx, "Tu launcher no deja crear accesos directos") }
            EnlaceRevista("Renombrar\u00A0→") { renombrar = true }
        }
        Filete(1.dp)
        Spacer(Modifier.height(16.dp))
        if (disenando) Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).background(Sand).padding(12.dp)) {
            Text("Toca una casilla para elegir qué va en ella (o texto libre, o una imagen). Arrastra el círculo ⇔ de la línea para moverla y el ⇕ de abajo para cambiar el alto.", color = Charcoal, fontFamily = Body, fontSize = 13.sp, lineHeight = 18.sp)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (j.portada.isNotBlank()) Text("USAR LA PORTADA AUTOMÁTICA", color = Acento, fontFamily = Body, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, modifier = Modifier.clickable { Biblioteca.poner(j.copy(portada = "")) }.padding(vertical = 6.dp))
                Spacer(Modifier.weight(1f))
                Text("QUITAR DE PÓRTICO", color = Muted, fontFamily = Body, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, modifier = Modifier.clickable { borrar = true }.padding(vertical = 6.dp))
            }
        }
        MagPage(j, defs(j, ctx, { selector = true }, { editarWeb = true }, { editarHoras = true }), disenando)
        if (!disenando) SeccionNube(j)
        if (disenando) MagFooter("Listo") { disenando = false } else MagFooter { disenando = true }
        Spacer(Modifier.height(20.dp))
    }
    if (renombrar) DialogoTexto("Nombre del juego", j.titulo, "Guardar", { renombrar = false }) { t -> if (t.isNotBlank()) Biblioteca.poner(j.copy(titulo = t.trim())); renombrar = false }
    if (borrar) AlertDialog(
        onDismissRequest = { borrar = false }, containerColor = Paper, shape = esquina(4.dp),
        title = { Text("¿Quitar «${j.titulo}»?", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold) },
        text = { Text(if (j.tipo == Tipo.ROM) "Solo se quita de Pórtico: el archivo de la ROM no se toca." else "Solo se quita de Pórtico.", color = Muted, fontFamily = Body) },
        confirmButton = { TextButton({ Biblioteca.quitar(j.id); borrar = false; alCerrar() }) { Text("QUITAR", color = Acento, fontFamily = Body, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp) } },
        dismissButton = { TextButton({ borrar = false }) { Text("CANCELAR", color = Charcoal, fontFamily = Body, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp) } }
    )
    if (eligePortada) AlertDialog(
        onDismissRequest = { eligePortada = false }, containerColor = Paper, shape = esquina(4.dp),
        title = { Text("Portada de «${j.titulo}»", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                MagLink("Buscar en internet", "Google Imágenes, Pinterest, ArtStation, Newgrounds") { eligePortada = false; buscaInternet = true }
                Filete(1.dp, Line)
                MagLink("Elegir de la galería") { eligePortada = false; cambiarPortada.launch("image/*") }
                if (j.portada.isNotBlank()) { Filete(1.dp, Line); MagLink("Usar la portada automática") { eligePortada = false; Biblioteca.poner(j.copy(portada = "")) } }
            }
        },
        confirmButton = {}, dismissButton = { TextButton({ eligePortada = false }) { Text("CERRAR", color = Charcoal, fontFamily = Body, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp) } }
    )
    if (buscaInternet) InternetPicker(
        initialQuery = j.titulo + (if (j.tipo == Tipo.ROM) " " + (sistema?.nombre ?: "") + " portada" else " portada"),
        idJuego = j.id,
        onPick = { ruta -> Biblioteca.por(j.id)?.let { Biblioteca.poner(it.copy(portada = ruta)) }; buscaInternet = false },
        onDismiss = { buscaInternet = false }
    )
    if (selector) SelectorEmulador(j.sistema, j) { selector = false }
    if (editarWeb) DialogoWeb(j) { editarWeb = false }
    if (editarHoras) DialogoHoras(j.msJugados, alAceptar = { ms -> Biblioteca.cambiar(j.id) { it.conTotalMs(ms) }; editarHoras = false }, alCerrar = { editarHoras = false })
}

/** Un cuadro con una línea de texto para escribir. */
@Composable
fun DialogoTexto(titulo: String, inicial: String, boton: String, alCerrar: () -> Unit, alAceptar: (String) -> Unit) {
    var t by remember { mutableStateOf(inicial) }
    AlertDialog(
        onDismissRequest = alCerrar, containerColor = Paper, shape = esquina(4.dp),
        title = { Text(titulo, color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold) },
        text = { Campo(t, "", { t = it }) },
        confirmButton = { TextButton({ alAceptar(t) }) { Text(boton.uppercase(), color = Acento, fontFamily = Body, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp) } },
        dismissButton = { TextButton(alCerrar) { Text("CANCELAR", color = Charcoal, fontFamily = Body, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp) } }
    )
}

/** Una línea de texto con un filete debajo (la casilla de la revista). */
@Composable
fun Campo(valor: String, pista: String, alCambiar: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            if (valor.isEmpty()) Text(pista, color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 18.sp)
            BasicTextField(valor, alCambiar, singleLine = true, textStyle = TextStyle(color = Charcoal, fontFamily = Display, fontSize = 18.sp), cursorBrush = SolidColor(Acento), modifier = Modifier.fillMaxWidth())
        }
        Filete(1.5.dp)
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
    ModalBottomSheet(alCerrar, containerColor = Paper, contentColor = Charcoal, shape = RectangleShape) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState())) {
            Antetitulo(if (juego != null) "Emulador de este juego" else "Emulador", Modifier.padding(bottom = 4.dp), color = Acento, conCuadro = false)
            FitTitle(sistema?.nombre ?: sistemaId, 34.sp, 36.sp, letterSpacing = (-1).sp, maxLines = 2)
            Cuerpo("Los que ya tienes instalados salen marcados. «Automático» usa uno propio (sin núcleos que descargar) y, si no hay, RetroArch.", tam = 12, modifier = Modifier.padding(vertical = 8.dp))
            Filete(2.dp)
            fun elegir(l: String) {
                actual = l
                if (juego != null) Biblioteca.poner(Biblioteca.por(juego.id)!!.copy(emulador = l))
                else { if (l.isEmpty()) Ajustes.emuladorPorSistema.remove(sistemaId) else Ajustes.emuladorPorSistema[sistemaId] = l; Ajustes.guardar(ctx) }
            }
            Opcion("Automático", "Recomendado", actual.isEmpty(), true, null) { elegir("") }
            if (evaluados.isEmpty()) Cuerpo("Pórtico todavía no sabe lanzar este sistema.", modifier = Modifier.padding(top = 12.dp))
            evaluados.forEach { e ->
                val r = e.resultado
                val sub = when (r) {
                    is Resultado.Ok -> if (Comando.esRetroArch(e.plantilla)) "Instalado · necesita el núcleo en RetroArch" else "Instalado"
                    is Resultado.Falta -> r.motivo
                }
                Opcion(e.etiqueta, sub, actual == e.etiqueta, r is Resultado.Ok, (r as? Resultado.Falta)?.paquete) { elegir(e.etiqueta) }
            }
        }
    }
}

@Composable
private fun Opcion(titulo: String, sub: String, elegida: Boolean, instalado: Boolean, paquetePlay: String?, alElegir: () -> Unit) {
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth().clickable(onClick = alElegir).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, color = if (elegida) Acento else Charcoal, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Text(sub.uppercase(), color = Muted, fontFamily = Body, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        if (!instalado && paquetePlay != null) Text("INSTALAR\u00A0→", color = Acento, fontFamily = Body, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.2.sp,
            modifier = Modifier.clickable { Lanzador.tienda(ctx, paquetePlay) }.padding(10.dp))
        else if (elegida) Text("✓", color = Acento, fontWeight = FontWeight.Black, fontSize = 18.sp)
        else if (instalado && titulo != "Automático") Text("INSTALADO", color = Muted, fontFamily = Body, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
    Filete(1.dp, Line)
}

/** Horas y minutos jugados, para corregirlos a mano. */
@Composable
fun DialogoHoras(ms: Long, alAceptar: (Long) -> Unit, alCerrar: () -> Unit) {
    var h by remember { mutableStateOf((ms / 3_600_000).toString()) }
    var m by remember { mutableStateOf(((ms / 60_000) % 60).toString()) }
    AlertDialog(
        onDismissRequest = alCerrar, containerColor = Paper, shape = esquina(4.dp),
        title = { Text("Horas jugadas", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) { Etiqueta("Horas"); Campo(h, "0", { h = it.filter(Char::isDigit).take(5) }) }
                    Column(Modifier.weight(1f)) { Etiqueta("Minutos"); Campo(m, "0", { m = it.filter(Char::isDigit).take(2) }) }
                }
                Cuerpo("Pórtico suma solo el tiempo que pasas fuera cuando abres un juego (aproximado). Aquí lo corriges.", tam = 12)
            }
        },
        confirmButton = { TextButton({ alAceptar((h.toLongOrNull() ?: 0L) * 3_600_000L + (m.toLongOrNull() ?: 0L).coerceAtMost(59L) * 60_000L) }) { Text("GUARDAR", color = Acento, fontFamily = Body, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp) } },
        dismissButton = { TextButton(alCerrar) { Text("CANCELAR", color = Charcoal, fontFamily = Body, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp) } }
    )
}

private fun fechaCorta(t: Long) = java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale("es", "ES")).format(java.util.Date(t))
fun notaTexto(n: Float) = if (n <= 0f) "—" else if (n % 1f == 0f) n.toInt().toString() else String.format(java.util.Locale("es", "ES"), "%.1f", n)

/** Los módulos de la ficha: lo que se puede poner en una casilla. */
@Composable
private fun defs(j: Juego, ctx: Context, abrirSelector: () -> Unit, editarWeb: () -> Unit, editarHoras: () -> Unit): Map<String, MagDef> = mapOf(
    "horas" to MagDef("Horas jugadas", "✎" to editarHoras) {
        FitTitle(tiempoDe(j.msJugados), 40.sp, 42.sp, unaLinea = true, maxLines = 1)
        Text("TUS HORAS", color = Muted, fontFamily = Body, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text("Aproximado: se apunta al volver de jugar", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
    },
    "veces" to MagDef("Veces y última vez") {
        Text(j.veces.toString(), color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 42.sp, maxLines = 1)
        Text("VECES", color = Muted, fontFamily = Body, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text("Última vez: " + haceCuanto(j.ultima).lowercase(), color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
    },
    "estado" to MagDef("Estado") {
        MagChoice(listOf("Sin terminar", "Terminado"), if (j.terminado) 1 else 0) { i ->
            Biblioteca.cambiar(j.id) { if (i == 1) it.copy(terminado = true, finalizado = System.currentTimeMillis()) else it.copy(terminado = false, finalizado = 0L) }
        }
        Text(if (j.terminado && j.finalizado > 0) "terminado el " + fechaCorta(j.finalizado) else if (j.terminado) "terminado" else "todavía en marcha", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
    },
    "valoracion" to MagDef("Valoración") {
        if (!j.terminado) {
            Text("Márcalo como terminado para valorarlo.", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 16.sp)
            MagLink("Marcar terminado", accent = true) { Biblioteca.cambiar(j.id) { it.copy(terminado = true, finalizado = System.currentTimeMillis()) } }
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(notaTexto(j.nota), color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 64.sp, lineHeight = 62.sp, letterSpacing = (-2).sp, maxLines = 1, softWrap = false)
                Text("/10", color = Muted, fontFamily = Body, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                EnlaceRevista("−\u00A00,5") { Biblioteca.cambiar(j.id) { it.copy(nota = (it.nota - 0.5f).coerceAtLeast(0f)) } }
                EnlaceRevista("+\u00A00,5") { Biblioteca.cambiar(j.id) { it.copy(nota = if (it.nota <= 0f) 7f else (it.nota + 0.5f).coerceAtMost(10f)) } }
            }
            MagChoice((1..10).map { it.toString() }, if (j.nota > 0f && j.nota % 1f == 0f) j.nota.toInt() - 1 else -1) { i -> Biblioteca.cambiar(j.id) { it.copy(nota = (i + 1).toFloat()) } }
        }
    },
    "apuntes" to MagDef("Apuntes") {
        var t by remember(j.id) { mutableStateOf(j.notas) }
        LaunchedEffect(t) { kotlinx.coroutines.delay(500); if (t != Biblioteca.por(j.id)?.notas) Biblioteca.cambiar(j.id) { it.copy(notas = t) } }
        Box(Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            if (t.isEmpty()) Text("Trucos, por dónde vas, qué te falta…", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 17.sp)
            BasicTextField(t, { t = it }, textStyle = TextStyle(color = Charcoal, fontFamily = Display, fontSize = 19.sp, lineHeight = 29.sp), cursorBrush = SolidColor(Acento), modifier = Modifier.fillMaxWidth())
        }
    },
    "lanzamiento" to MagDef("Cómo se abre", if (j.tipo == Tipo.WEB) "Editar" to editarWeb else if (j.tipo == Tipo.ROM) "Cambiar" to abrirSelector else null) {
        when (j.tipo) {
            Tipo.ROM -> {
                val e = remember(j) { Lanzador.elegir(ctx, j) }
                Text(e?.etiqueta ?: "Sin emulador para este sistema", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                val r = e?.resultado
                Text((when (r) { is Resultado.Ok -> if (Comando.esRetroArch(e.plantilla)) "Instalado · necesita el núcleo descargado en RetroArch" else "Instalado y listo"; is Resultado.Falta -> r.motivo; null -> "" }).uppercase(),
                    color = if (r is Resultado.Ok) Muted else Acento, fontFamily = Body, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Tipo.APP -> { Text("App de Android", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp); Text(j.paquete, color = Muted, fontFamily = Body, fontSize = 12.sp) }
            Tipo.WEB -> { Text(j.url, color = Charcoal, fontFamily = Display, fontSize = 17.sp, maxLines = 2); Text(("Pantalla: " + listOf("automática", "horizontal", "vertical")[j.orientacion.coerceIn(0, 2)]).uppercase(), color = Muted, fontFamily = Body, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
        }
    },
)


/** «En la nube» (solo ROMs): carpeta de guardados del sistema, subir la ROM a tu kDrive y bajarla en otro móvil. */
@Composable
private fun SeccionNube(j: Juego) {
    if (j.tipo != Tipo.ROM) return
    val ctx = LocalContext.current
    val sistema = Catalogo.sistema(j.sistema)
    val listo = Cuenta.vinculada && KDrive.on(ctx)
    var mensaje by remember(j.id) { mutableStateOf<String?>(null) }
    var ocupado by remember(j.id) { mutableStateOf(false) }
    var nube by remember(j.id, j.uri) { mutableStateOf<NubeP.Ficha?>(null) }
    LaunchedEffect(j.id, j.uri, listo) { if (listo) nube = withContext(Dispatchers.IO) { NubeP.romEnNube(j) } }
    val carpeta = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { u -> if (u != null) NubeP.ponCarpeta(ctx, j.sistema, u) }
    val destinoRom = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { u ->
        val f = nube
        if (u != null && f != null && !ocupado) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(u, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            ocupado = true; mensaje = "Bajando la ROM…"
            kotlin.concurrent.thread {
                val r = runCatching {
                    val a = NubeP.bajarRom(ctx.applicationContext, j, f, DocCarpeta(ctx.applicationContext, androidx.documentfile.provider.DocumentFile.fromTreeUri(ctx, u)!!))
                    val doc = runCatching { android.provider.DocumentsContract.getDocumentId(android.net.Uri.parse(a.uri)) }.getOrDefault("")
                    Biblioteca.por(j.id)?.let { Biblioteca.poner(it.copy(uri = a.uri, ruta = Comando.rutaDeDocId(doc, "/storage/emulated/0").orEmpty())) }
                }
                android.os.Handler(android.os.Looper.getMainLooper()).post { mensaje = r.fold({ "ROM descargada" }, { "No se pudo bajar: ${it.message}" }); ocupado = false }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Filete(2.dp)
    Spacer(Modifier.height(12.dp))
    Antetitulo("En la nube", Modifier.padding(bottom = 4.dp))
    if (!listo) {
        Cuerpo("Para llevarte la partida a otro móvil: vincula tu cuenta de Atrio y conecta tu kDrive (Ajustes). Pórtico sube el guardado al volver de jugar y baja el último antes de jugar.", tam = 12)
    } else {
        val car = NubeP.carpetas[j.sistema]
        MagLink(if (car == null) "Elegir la carpeta de guardados de ${sistema?.corto ?: j.sistema}" else "Carpeta de guardados elegida · cambiar",
            "La carpeta donde ${sistema?.corto ?: "el emulador"} deja los .srm / .sav. Solo se sincronizan los de esta ROM (mismo nombre que el archivo).") { carpeta.launch(null) }
        Filete(1.dp, Line)
        when {
            j.enOtroDispositivo && nube != null -> MagLink(if (ocupado) "Descargando…" else "Descargar la ROM de mi kDrive", "Elige la carpeta del móvil donde dejarla") { if (!ocupado) destinoRom.launch(null) }
            j.enOtroDispositivo -> Cuerpo("Esta ROM está en otro de tus móviles y todavía no se ha subido a tu kDrive.", tam = 12)
            nube != null -> Cuerpo("La ROM ya está en tu kDrive.", tam = 12)
            else -> MagLink(if (ocupado) "Subiendo…" else "Subir la ROM a mi kDrive", "Solo a tu kDrive, para tenerla en tus otros móviles") {
                if (!ocupado && j.uri.isNotBlank()) {
                    ocupado = true; mensaje = "Subiendo la ROM…"
                    kotlin.concurrent.thread {
                        val r = runCatching {
                            val d = androidx.documentfile.provider.DocumentFile.fromSingleUri(ctx, android.net.Uri.parse(j.uri)) ?: error("no encuentro el archivo")
                            NubeP.subirRom(ctx.applicationContext, j, DocArch(ctx.applicationContext, d))
                        }
                        android.os.Handler(android.os.Looper.getMainLooper()).post { mensaje = r.fold({ "ROM subida a tu kDrive" }, { "No se pudo subir: ${it.message}" }); ocupado = false; nube = null }
                    }
                }
            }
        }
    }
    mensaje?.let { Text(it, color = Acento, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp)) }
}
