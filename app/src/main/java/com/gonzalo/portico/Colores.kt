package com.gonzalo.portico

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** «#RRGGBB» (o «RRGGBB») → color opaco; null si no es un código válido. */
fun hexAColor(t: String): Int? = t.trim().removePrefix("#").takeIf { it.length == 6 }?.toIntOrNull(16)?.let { 0xFF000000.toInt() or it }
fun colorAHex(c: Int) = "#%06X".format(c and 0xFFFFFF)

/** Elegir un color cualquiera: matiz, intensidad y brillo (o el código #RRGGBB). */
@Composable
fun DialogoColor(inicial: Int, alCerrar: () -> Unit, alElegir: (Int) -> Unit) {
    val hsv = remember(inicial) { FloatArray(3).also { android.graphics.Color.colorToHSV(inicial, it) } }
    var h by remember(inicial) { mutableStateOf(hsv[0]) }
    var sat by remember(inicial) { mutableStateOf(hsv[1]) }
    var v by remember(inicial) { mutableStateOf(hsv[2]) }
    var exacto by remember(inicial) { mutableStateOf<Int?>(null) }
    val color = exacto ?: android.graphics.Color.HSVToColor(floatArrayOf(h, sat, v))
    var hex by remember(inicial) { mutableStateOf(colorAHex(inicial)) }
    fun muestra(nh: Float, ns: Float, nv: Float) { exacto = null; hex = colorAHex(android.graphics.Color.HSVToColor(floatArrayOf(nh, ns, nv))) }
    val colores = SliderDefaults.colors(thumbColor = Acento, activeTrackColor = Acento, inactiveTrackColor = Sand)
    AlertDialog(
        onDismissRequest = alCerrar, containerColor = Paper, shape = esquina(4.dp),
        title = { Text("Color", color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(Color(color)).border(1.dp, Line, RoundedCornerShape(12.dp)))
                Etiqueta("Matiz", Modifier.padding(top = 10.dp))
                Slider(h, { h = it; muestra(it, sat, v) }, valueRange = 0f..360f, colors = colores)
                Etiqueta("Intensidad")
                Slider(sat, { sat = it; muestra(h, it, v) }, valueRange = 0f..1f, colors = colores)
                Etiqueta("Brillo")
                Slider(v, { v = it; muestra(h, sat, it) }, valueRange = 0f..1f, colors = colores)
                CampoCodigo(hex) { t, c ->
                    hex = t
                    if (c != null) { exacto = c.toArgb(); val f = FloatArray(3); android.graphics.Color.colorToHSV(c.toArgb(), f); h = f[0]; sat = f[1]; v = f[2] }
                }
            }
        },
        confirmButton = { TextButton({ alElegir(color) }) { Text("USAR", color = Acento, fontFamily = Body, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp) } },
        dismissButton = { TextButton(alCerrar) { Text("CANCELAR", color = Charcoal, fontFamily = Body, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp) } }
    )
}
