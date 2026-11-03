package com.gonzalo.portico

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val VERSION = "0.5.0"

private fun aliasInicio(c: Context) = ComponentName(c, "com.gonzalo.portico.InicioAlias")
private fun esInicio(c: Context) = c.packageManager.getComponentEnabledSetting(aliasInicio(c)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

/** «Ajustes»: Apariencia (modo, acento, tamaño, estilo), Portadas, Pantalla de inicio y copias. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantEstilo(alVolver: () -> Unit) {
    val ctx = LocalContext.current
    var inicio by remember { mutableStateOf(esInicio(ctx)) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    fun cambia(f: () -> Unit) { f(); Skin.guardar(ctx) }
    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u ->
        if (u != null) mensaje = runCatching { ctx.contentResolver.openOutputStream(u)!!.use { it.write(Biblioteca.aTexto().toByteArray()) }; "Copia guardada" }.getOrElse { "No se pudo guardar la copia" }
    }
    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) mensaje = runCatching { Biblioteca.leer(ctx.contentResolver.openInputStream(u)!!.use { it.readBytes().decodeToString() }, reemplazar = false); Biblioteca.guardar(); "Copia importada" }.getOrElse { "Ese archivo no es una copia de Pórtico" }
    }
    Column(Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
        RevistaTopBar(alVolver, "Pórtico")
        Antetitulo("Sección", Modifier.padding(top = 10.dp), color = Acento, conCuadro = false)
        FitTitle("Ajustes", 52.sp, 56.sp, letterSpacing = (-1.5).sp, modifier = Modifier.padding(top = 2.dp), unaLinea = true)
        Filete(3.dp)
        Spacer(Modifier.height(14.dp))

        Antetitulo("Apariencia", Modifier.padding(bottom = 4.dp))
        if (!Skin.origenAtrio) {
            Etiqueta("Modo", Modifier.padding(top = 8.dp))
            MagChoice(listOf("Como el sistema", "Claro", "Oscuro"), Modo.eleccion) { i -> cambia { Modo.eleccion = i } }
        }
        Etiqueta("Colores de", Modifier.padding(top = 10.dp))
        MagChoice(listOf("Pórtico", "Atrio"), if (Skin.origenAtrio) 1 else 0) { i -> cambia { Skin.origenAtrio = i == 1 } }
        Etiqueta("Invertir", Modifier.padding(top = 6.dp))
        MagChoice(listOf("No", "Sí"), if (Skin.invertido) 1 else 0) { i -> cambia { Skin.invertido = i == 1 } }
        LaunchedEffect(Unit) { kotlin.concurrent.thread { TemaAtrio.refrescar(ctx.applicationContext) } }
        if (Skin.origenAtrio) {
            val leidos = TemaAtrio.tonos != null
            Text(
                if (TemaAtrio.aviso.isNotBlank()) TemaAtrio.aviso + (if (leidos) " Se usan los últimos colores que se leyeron." else " Mientras tanto, los de Pórtico.")
                else "Pórtico lee los colores que tienes ahora en Atrio" + (if (Skin.invertido) " y los pone del revés: el fondo con el color del texto y el texto con el del fondo; el acento se mantiene." else "."),
                color = if (TemaAtrio.aviso.isNotBlank()) Acento else Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 14.sp, lineHeight = 19.sp
            )
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("Fondo" to Paper, "Texto" to Charcoal, "Acento" to Acento).forEach { (n, c) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(34.dp).background(c).border(1.dp, Line)); Text(n.uppercase(), color = Muted, fontFamily = Body, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                }
            }
        } else Text("Invertir pone el fondo con el color del texto y el texto con el del fondo; el acento se queda.", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 14.sp, lineHeight = 19.sp)
        if (!Skin.origenAtrio) {
        Etiqueta("Acento", Modifier.padding(top = 10.dp, bottom = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("EL DE LA PALETA", color = if (Skin.accentId == "paleta") Acento else Charcoal, fontFamily = Body, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp,
                modifier = Modifier.clickable { cambia { Skin.accentId = "paleta" } }.padding(end = 6.dp, top = 8.dp, bottom = 8.dp))
            ACCENT_COLORS.forEach { a ->
                val on = Skin.accentId == a.id
                Box(Modifier.size(30.dp).background(Color((if (Modo.oscuro) a.dark else a.light) or 0xFF000000)).border(if (on) 3.dp else 0.dp, Charcoal).clickable { cambia { Skin.accentId = a.id } })
            }
        }
        }
        Etiqueta("Tamaño del texto", Modifier.padding(top = 14.dp))
        MagChoice(listOf("Pequeño", "Normal", "Grande", "Muy grande"), listOf(90, 100, 112, 125).indexOf(Skin.escala).coerceAtLeast(1)) { i -> cambia { Skin.escala = listOf(90, 100, 112, 125)[i] } }
        Etiqueta("Estilo", Modifier.padding(top = 14.dp))
        var masEstilos by remember { mutableStateOf(false) }
        if (masEstilos) MagChoice(STYLES.map { it.name }, STYLES.indexOfFirst { it.id == Skin.estiloActual()?.id }) { i -> cambia { Skin.aplicar(STYLES[i]) } }
        else Text(Skin.estiloActual()?.name ?: "Combinación propia", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp))
        EnlaceRevista(if (masEstilos) "Ocultar estilos\u00A0→" else "Cambiar de estilo\u00A0→", acento = true) { masEstilos = !masEstilos }
        Text(Skin.estiloActual()?.desc ?: "", color = Muted, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
        Text("Los estilos y paletas son los mismos que los de Atrio.", color = Muted, fontFamily = Body, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(26.dp))
        Filete(2.dp)
        Spacer(Modifier.height(12.dp))
        Antetitulo("Portadas", Modifier.padding(bottom = 4.dp))
        Fila("Portadas automáticas", "Busca la caja de cada ROM en thumbnails.libretro.com (se envía el nombre del archivo). Si lo desactivas no sale nada a internet.") {
            Interruptor(Ajustes.portadasLibretro) { Ajustes.portadasLibretro = it; Ajustes.guardar(ctx) }
        }

        Spacer(Modifier.height(14.dp))
        Filete(2.dp)
        Spacer(Modifier.height(12.dp))
        Antetitulo("Pantalla de inicio", Modifier.padding(bottom = 4.dp))
        Fila("Usar Pórtico como launcher", if (inicio) "Pórtico ya puede elegirse como pantalla de inicio del móvil." else "Permite elegir Pórtico como pantalla de inicio (Android te lo pregunta).") {
            Interruptor(inicio) { on ->
                ctx.packageManager.setComponentEnabledSetting(aliasInicio(ctx), if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                inicio = on
                if (on) runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }

        Spacer(Modifier.height(14.dp))
        Filete(2.dp)
        Spacer(Modifier.height(12.dp))
        Antetitulo("Copias", Modifier.padding(bottom = 4.dp))
        MagLink("Guardar copia de la biblioteca", "Un archivo con tus juegos y carpetas (no incluye las ROMs)") { exportar.launch("portico-biblioteca.json") }
        Filete(1.dp, Line)
        MagLink("Importar copia", "Suma los juegos de una copia a los que ya tienes") { importar.launch(arrayOf("application/json", "*/*")) }
        mensaje?.let { Text(it, color = Acento, fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp)) }

        Spacer(Modifier.height(26.dp))
        Filete(3.dp)
        Spacer(Modifier.height(10.dp))
        Etiqueta("Pórtico $VERSION")
        Text("Hermana de Atrio. Los comandos para abrir emuladores salen de ES-DE Frontend (licencia MIT, © Northwestern Software AB, Leon Styhre y Alec Lofquist). Las portadas, de libretro.", color = Muted, fontFamily = Body, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(30.dp))
    }
}
