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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val VERSION = "0.2.0"

private fun aliasInicio(c: Context) = ComponentName(c, "com.gonzalo.portico.InicioAlias")
private fun esInicio(c: Context) = c.packageManager.getComponentEnabledSetting(aliasInicio(c)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantEstilo() {
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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = arriba(), bottom = 130.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Titulo("Estilo", 34)
                Cuerpo("Pórtico usa los mismos temas, paletas y letras que Atrio: elige uno y las dos apps se verán como de la misma familia.", tam = 13)
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Etiqueta("Modo")
                Row { listOf("Como el sistema", "Claro", "Oscuro").forEachIndexed { i, t -> Chip(t, Modo.eleccion == i) { cambia { Modo.eleccion = i } } } }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Etiqueta("Estilos", Modifier.padding(horizontal = 20.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(STYLES, key = { it.id }) { s -> TarjetaEstilo(s, Skin.estiloActual()?.id == s.id) { cambia { Skin.aplicar(s) } } }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Etiqueta("Paletas")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PALETTES.forEach { p ->
                        val t = if (Modo.oscuro) p.dark else p.light
                        val on = Skin.palette.id == p.id
                        Box(Modifier.size(46.dp).clip(CircleShape).background(t.cPaper).border(if (on) 3.dp else 1.dp, if (on) Acento else Line, CircleShape).clickable { cambia { Skin.palette = p } }, contentAlignment = Alignment.Center) {
                            Box(Modifier.size(18.dp).clip(CircleShape).background(Color(t.general or 0xFF000000)))
                        }
                    }
                }
                Cuerpo(Skin.palette.name, tam = 12)
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Etiqueta("Acento")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("El de la paleta", Skin.accentId == "paleta") { cambia { Skin.accentId = "paleta" } }
                    ACCENT_COLORS.forEach { a ->
                        val on = Skin.accentId == a.id
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color((if (Modo.oscuro) a.dark else a.light) or 0xFF000000)).border(if (on) 3.dp else 0.dp, Charcoal, CircleShape).clickable { cambia { Skin.accentId = a.id } })
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Etiqueta("Tamaño del texto")
                Row { listOf(90 to "Pequeño", 100 to "Normal", 112 to "Grande", 125 to "Muy grande").forEach { (p, n) -> Chip(n, Skin.escala == p) { cambia { Skin.escala = p } } } }
                Etiqueta("Portadas")
                Row { listOf("Normales", "Grandes", "Pequeñas").forEachIndexed { i, n -> Chip(n, Ajustes.tamano == i) { Ajustes.tamano = i; Ajustes.guardar(ctx) } } }
            }
        }
        item {
            Panel(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                Fila("Portadas automáticas", "Busca la caja de cada ROM en thumbnails.libretro.com (se envía el nombre del archivo). Si lo desactivas no sale nada a internet.", Icons.Filled.CloudDownload) {
                    Interruptor(Ajustes.portadasLibretro) { Ajustes.portadasLibretro = it; Ajustes.guardar(ctx) }
                }
                Fila("Pantalla de inicio", if (inicio) "Pórtico ya puede elegirse como launcher de Android." else "Permite elegir Pórtico como pantalla de inicio del móvil (Android te lo pregunta).", Icons.Filled.Home) {
                    Interruptor(inicio) { on ->
                        ctx.packageManager.setComponentEnabledSetting(aliasInicio(ctx), if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                        inicio = on
                        if (on) runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                }
                Fila("Guardar copia de la biblioteca", "Un archivo con tus juegos y carpetas (no incluye las ROMs).", Icons.Filled.Upload, alHacer = { exportar.launch("portico-biblioteca.json") })
                Fila("Importar copia", "Suma los juegos de una copia a los que ya tienes.", Icons.Filled.Download, alHacer = { importar.launch(arrayOf("application/json", "*/*")) })
                mensaje?.let { Cuerpo(it, tam = 12, color = Acento, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Etiqueta("Pórtico $VERSION")
                Cuerpo("Hermana de Atrio. Los comandos para abrir emuladores salen de ES-DE Frontend (licencia MIT, © Northwestern Software AB, Leon Styhre y Alec Lofquist). Las portadas, de libretro.", tam = 12)
            }
        }
    }
}

@Composable
private fun TarjetaEstilo(s: SkinStyle, elegido: Boolean, alHacer: () -> Unit) {
    val p = PALETTES.firstOrNull { it.id == s.palette } ?: PALETTES[0]
    val t = if (Modo.oscuro) p.dark else p.light
    val familia = Skin.family(s.display) ?: AtrioDisplay
    val f = FORMAS.firstOrNull { it.id == s.form } ?: FORMAS[0]
    Column(
        Modifier.width(150.dp).clip(esquina(22.dp)).background(t.cPaper).border(if (elegido) 3.dp else 1.dp, if (elegido) Acento else Line, esquina(22.dp)).clickable(onClick = alHacer).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp, 34.dp).clip(ArcoShape(f)).background(Color(t.general or 0xFF000000)))
            Box(Modifier.size(28.dp, 34.dp).clip(ArcoShape(f)).background(t.cCrudo))
            Box(Modifier.size(28.dp, 34.dp).clip(ArcoShape(f)).background(t.cSand))
        }
        Text(s.name, color = t.cInk, fontFamily = familia, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
        Text(s.desc, color = t.cMuted, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 2)
    }
}
