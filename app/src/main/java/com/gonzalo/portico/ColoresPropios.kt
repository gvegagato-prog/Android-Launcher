package com.gonzalo.portico

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Apariencia → A tu manera (como en Atrio): el color que quieras para el fondo, el texto y el acento (tono, saturación y brillo, o su
 * código). Se ve al momento; se guarda al soltar. Todo lo demás (tarjetas, líneas, grises) sale de ellos.
 */

private fun hexDe(c: Color) = "#%06X".format(c.toArgb() and 0xFFFFFF)
private fun largo(c: Color) = c.toArgb().toLong() and 0xFFFFFFFFL

/** Un color elegido a mano: la muestra y su código; al tocarlo se abren el tono, la saturación, el brillo y el código. */
@Composable
fun SelectorColor(nombre: String, valor: Long, actual: Color, onCambia: (Long) -> Unit, onGuarda: () -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable { abierto = !abierto }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(actual).border(1.dp, Charcoal))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(nombre, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Charcoal)
            Text(if (valor == 0L) "Automático" else hexDe(Color(valor)), color = Muted, fontFamily = Body, fontSize = 12.sp)
        }
        if (valor != 0L) EnlaceRevista("Quitar", Modifier.padding(end = 12.dp)) { onCambia(0L); onGuarda(); abierto = false }
        Text(if (abierto) "▴" else "▾", color = Muted, fontSize = 16.sp)
    }
    if (!abierto) return
    // Tono, saturación y brillo de lo que se ve ahora (el automático, si aún no hay ninguno elegido)
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(actual.toArgb(), it) } }
    var h by remember { mutableFloatStateOf(hsv[0]) }
    var s by remember { mutableFloatStateOf(hsv[1]) }
    var v by remember { mutableFloatStateOf(hsv[2]) }
    var texto by remember { mutableStateOf(hexDe(actual)) }
    fun pon() { val c = Color.hsv(h, s, v); texto = hexDe(c); onCambia(largo(c)) }
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Barra("Tono", h / 360f, Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) }), { h = it * 360f; pon() }, onGuarda)
        Barra("Saturación", s, Brush.horizontalGradient(listOf(Color.hsv(h, 0f, v), Color.hsv(h, 1f, v))), { s = it; pon() }, onGuarda)
        Barra("Brillo", v, Brush.horizontalGradient(listOf(Color.Black, Color.hsv(h, s, 1f))), { v = it; pon() }, onGuarda)
        Etiqueta("Código (#RRGGBB)", Modifier.padding(top = 6.dp))
        Campo(texto, "#RRGGBB", { t ->
            texto = t.uppercase().take(7)
            Regex("^#?([0-9A-F]{6})$").find(texto)?.let { m ->
                val c = Color(("FF" + m.groupValues[1]).toLong(16))
                FloatArray(3).also { android.graphics.Color.colorToHSV(c.toArgb(), it) }.let { x -> h = x[0]; s = x[1]; v = x[2] }
                onCambia(largo(c)); onGuarda()
            }
        })
    }
}

/** Una barra con su degradado debajo (tono, saturación o brillo). */
@Composable
private fun Barra(nombre: String, valor: Float, fondo: Brush, onCambia: (Float) -> Unit, onSuelta: () -> Unit) {
    Column {
        Text(nombre.uppercase(), color = Muted, fontFamily = Body, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(12.dp).background(fondo).border(1.dp, Line))
            Slider(valor, onCambia, onValueChangeFinished = onSuelta, modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(thumbColor = Charcoal, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent))
        }
    }
}
