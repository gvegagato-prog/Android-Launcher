package com.gonzalo.portico

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Texto con la letra de títulos. */
@Composable
fun Titulo(t: String, tam: Int = 28, modifier: Modifier = Modifier, color: Color = Charcoal, lineas: Int = 2) =
    Text(t, modifier, color = color, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = tam.sp, lineHeight = (tam * 1.1f).sp, maxLines = lineas, overflow = TextOverflow.Ellipsis)

@Composable
fun Etiqueta(t: String, modifier: Modifier = Modifier, color: Color = Muted) =
    Text(t.uppercase(), modifier, color = color, fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.2.sp)

@Composable
fun Cuerpo(t: String, modifier: Modifier = Modifier, color: Color = Muted, tam: Int = 14, lineas: Int = Int.MAX_VALUE) =
    Text(t, modifier, color = color, fontFamily = Body, fontSize = tam.sp, lineHeight = (tam * 1.4f).sp, maxLines = lineas, overflow = TextOverflow.Ellipsis)

/** Botón principal: píldora con el acento. */
@Composable
fun BotonAcento(texto: String, icono: ImageVector? = null, modifier: Modifier = Modifier, alto: Dp = 52.dp, alHacer: () -> Unit) {
    Row(
        modifier.height(alto).shadow(if (Skin.forma.sombraDura) 0.dp else 0.dp).clip(pildora()).background(Acento).clickable(onClick = alHacer).padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        if (icono != null) { Icon(icono, null, tint = SobreAcento, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)) }
        Text(texto, color = SobreAcento, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

/** Botón secundario: píldora de relleno suave. */
@Composable
fun BotonSuave(texto: String, icono: ImageVector? = null, modifier: Modifier = Modifier, peligro: Boolean = false, alHacer: () -> Unit) {
    val col = if (peligro) Color(0xFFE5484D) else Charcoal
    Row(
        modifier.height(44.dp).clip(pildora()).background(Sand).clickable(onClick = alHacer).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        if (icono != null) { Icon(icono, null, tint = col, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(texto, color = col, fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

/** Botón redondo con icono. */
@Composable
fun BotonIcono(icono: ImageVector, descripcion: String, modifier: Modifier = Modifier, fondo: Color = Sand, tinta: Color = Charcoal, tam: Dp = 44.dp, alHacer: () -> Unit) {
    Box(
        modifier.size(tam).clip(if (Skin.forma.esq == Esq.Redonda) CircleShape else esquina(12.dp)).background(fondo).clickable(onClick = alHacer).semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center
    ) { Icon(icono, null, tint = tinta, modifier = Modifier.size(tam * 0.5f)) }
}

/** Chip de filtro: seleccionado = tinta llena (como en Atrio). */
@Composable
fun Chip(texto: String, activo: Boolean, modifier: Modifier = Modifier, cuenta: Int? = null, alHacer: () -> Unit) {
    val fondo by animateColorAsState(if (activo) Charcoal else Color.Transparent, label = "chip")
    val tinta = if (activo) Paper else Muted
    Row(
        modifier.height(36.dp).clip(pildora()).background(fondo).clickable(onClick = alHacer).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto, color = tinta, fontFamily = Body, fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp)
        if (cuenta != null) { Spacer(Modifier.width(5.dp)); Text("$cuenta", color = tinta.copy(alpha = 0.6f), fontFamily = Body, fontSize = 11.sp) }
    }
}

/** Panel de superficie (tarjeta). */
@Composable
fun Panel(modifier: Modifier = Modifier, alHacer: (() -> Unit)? = null, contenido: @Composable ColumnScope.() -> Unit) {
    val f = Skin.forma
    val forma = esquina(22.dp)
    Column(
        modifier.clip(forma).background(Crudo).let { if (f.borde.value > 0f) it.border(f.borde, if (f.bordeTinta) Charcoal else Line, forma) else it }
            .let { if (alHacer != null) it.clickable(onClick = alHacer) else it }.padding(16.dp),
        content = contenido
    )
}

/** Una fila de ajuste: icono, título, descripción y algo a la derecha. */
@Composable
fun Fila(titulo: String, descripcion: String? = null, icono: ImageVector? = null, modifier: Modifier = Modifier, alHacer: (() -> Unit)? = null, final: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().let { if (alHacer != null) it.clip(esquina(14.dp)).clickable(onClick = alHacer) else it }.padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icono != null) { Icon(icono, null, tint = Acento, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(14.dp)) }
        Column(Modifier.weight(1f)) {
            Text(titulo, color = Charcoal, fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (descripcion != null) Cuerpo(descripcion, tam = 12)
        }
        if (final != null) { Spacer(Modifier.width(10.dp)); final() }
    }
}

/** Interruptor con el acento. */
@Composable
fun Interruptor(activo: Boolean, alCambiar: (Boolean) -> Unit) {
    val fondo by animateColorAsState(if (activo) Acento else Sand, label = "sw")
    Box(Modifier.width(48.dp).height(28.dp).clip(RoundedCornerShape(50)).background(fondo).clickable(role = Role.Switch) { alCambiar(!activo) }.padding(3.dp)) {
        Box(Modifier.align(if (activo) Alignment.CenterEnd else Alignment.CenterStart).size(22.dp).clip(CircleShape).background(if (activo) SobreAcento else Muted))
    }
}

class Destino(val icono: ImageVector, val nombre: String)

/**
 * El dock de Pórtico: una píldora flotante con los destinos y, en medio, el «+» de añadir en el color de acento.
 * (En Atrio el dock es una píldora con cinco destinos; en Pórtico la mitad izquierda y la derecha se reparten el «+».)
 */
@Composable
fun Dock(destinos: List<Destino>, actual: Int, alElegir: (Int) -> Unit, alAnadir: () -> Unit, modifier: Modifier = Modifier) {
    val forma = if (Skin.forma.esq == Esq.Redonda) RoundedCornerShape(30.dp) else esquina(18.dp)
    Row(
        modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth().height(64.dp)
            .shadow(14.dp, forma, ambientColor = Color(0x44000000), spotColor = Color(0x44000000)).clip(forma).background(Crudo)
            .border(1.dp, Line, forma).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        destinos.forEachIndexed { i, d ->
            if (i == destinos.size / 2) {
                Box(
                    Modifier.size(52.dp).clip(if (Skin.forma.esq == Esq.Redonda) CircleShape else esquina(14.dp)).background(Acento).clickable(onClick = alAnadir).semantics { contentDescription = "Añadir juegos" },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Add, null, tint = SobreAcento, modifier = Modifier.size(28.dp)) }
            }
            val on = i == actual
            val fondo by animateColorAsState(if (on) Charcoal else Color.Transparent, label = "dock")
            Row(
                Modifier.height(46.dp).animateContentSize().clip(pildora()).background(fondo)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { alElegir(i) }
                    .padding(horizontal = if (on) 14.dp else 12.dp).semantics { contentDescription = d.nombre },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(d.icono, null, tint = if (on) Paper else Muted, modifier = Modifier.size(24.dp))
                if (on) { Spacer(Modifier.width(6.dp)); Text(d.nombre, color = Paper, fontFamily = Body, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1) }
            }
        }
    }
}

val DESTINOS = listOf(
    Destino(Icons.Filled.Home, "Inicio"),
    Destino(Icons.Filled.VideogameAsset, "Juegos"),
    Destino(Icons.Filled.Memory, "Sistemas"),
    Destino(Icons.Filled.Palette, "Estilo"),
)

/** Cabecera de sección con «Ver todo». */
@Composable
fun Seccion(titulo: String, detalle: String? = null, alVerTodo: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(titulo, color = Charcoal, fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            if (detalle != null) Cuerpo(detalle, tam = 12)
        }
        if (alVerTodo != null) Text("Ver todo", color = Acento, fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.clip(pildora()).clickable(onClick = alVerTodo).padding(8.dp))
    }
}
