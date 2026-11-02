package com.gonzalo.portico

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GFont
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Formas: cómo dibuja cada estilo las esquinas y los bordes. Son las mismas ideas que las «formas» de Atrio, más simples:
 * en Pórtico la portada es un ARCO (la puerta de Atrio, repetida), y según la forma el arco es redondo, biselado o recto.
 */
enum class Esq { Redonda, Cortada, Recta }
data class Forma(val id: String, val esq: Esq, val radio: Float, val borde: Dp, val bordeTinta: Boolean, val sombraDura: Boolean, val arco: Float)

val FORMAS: List<Forma> = listOf(
    Forma("vitrina", Esq.Redonda, 1.3f, 0.dp, false, false, 1f),
    Forma("clasico", Esq.Redonda, 1f, 1.dp, false, false, 0.85f),
    Forma("editorial", Esq.Redonda, 0.3f, 1.dp, false, false, 0.55f),
    Forma("suave", Esq.Redonda, 1.8f, 0.dp, false, false, 1f),
    Forma("minimal", Esq.Redonda, 0.5f, 0.dp, false, false, 0.35f),
    Forma("comic", Esq.Redonda, 0.7f, 2.5.dp, true, true, 0.7f),
    Forma("neon", Esq.Cortada, 0.6f, 1.5.dp, false, false, 0.6f),
    Forma("militar", Esq.Cortada, 0.4f, 2.dp, true, false, 0.5f),
    Forma("pixel", Esq.Recta, 0f, 3.dp, true, true, 0f),
    Forma("terminal", Esq.Recta, 0f, 1.dp, true, false, 0f),
    Forma("brutal", Esq.Recta, 0f, 3.dp, true, true, 0f),
    Forma("revista", Esq.Recta, 0f, 0.dp, false, false, 0f),
)

/** Esquina normal de radio [r] según la forma. */
fun esquina(r: Dp): Shape {
    val f = Skin.forma
    val k = (r.value * f.radio).dp
    return when (f.esq) {
        Esq.Recta -> if (k.value < 0.5f) RectangleShape else RoundedCornerShape(k)
        Esq.Cortada -> if (k.value < 0.5f) RectangleShape else CutCornerShape(k)
        Esq.Redonda -> RoundedCornerShape(k)
    }
}

/** Botones y chips «píldora». */
fun pildora(): Shape = when (Skin.forma.esq) {
    Esq.Redonda -> if (Skin.forma.radio < 0.6f) RoundedCornerShape(10.dp) else RoundedCornerShape(50)
    Esq.Cortada -> CutCornerShape(30)
    Esq.Recta -> RectangleShape
}

/**
 * La portada de Pórtico: un arco. Arriba, media circunferencia (o menos, según la forma); abajo, esquinas pequeñas.
 * En las formas rectas es un rectángulo con borde; en las biseladas, el arco es un chaflán.
 */
class ArcoShape(private val f: Forma) : Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width; val h = size.height
        val abajo = (minOf(w, h) * 0.07f * f.radio.coerceAtLeast(0.6f)).coerceAtMost(minOf(w, h) / 4f)
        return when (f.esq) {
            Esq.Recta -> Outline.Rectangle(Rect(0f, 0f, w, h))
            Esq.Cortada -> {
                val c = minOf(w * 0.28f * f.arco.coerceAtLeast(0.5f), h / 3f)
                Outline.Generic(Path().apply {
                    moveTo(c, 0f); lineTo(w - c, 0f); lineTo(w, c); lineTo(w, h); lineTo(0f, h); lineTo(0f, c); close()
                })
            }
            Esq.Redonda -> {
                val r = (w / 2f * f.arco).coerceAtMost(h / 2f)
                Outline.Rounded(RoundRect(0f, 0f, w, h, CornerRadius(r), CornerRadius(r), CornerRadius(abajo), CornerRadius(abajo)))
            }
        }
    }
}

/** Mismo arco, más pequeño en el radio de las esquinas de abajo y menos alto: para el cartel del juego (apaisado). */
fun arco(): Shape = ArcoShape(Skin.forma)

/** Estado del modo claro/oscuro: lo pone la raíz de la app según Ajustes y el sistema. */
object Modo {
    var oscuro by mutableStateOf(true)
    /** 0 como el sistema, 1 claro, 2 oscuro. */
    var eleccion by mutableStateOf(2)
}

object Skin {
    var palette by mutableStateOf(PALETTES.first { it.id == "vitrina" })
    var display by mutableStateOf("Vitrina")
    var body by mutableStateOf("Atrio")
    var forma by mutableStateOf(FORMAS.first { it.id == "vitrina" })
    /** Id de [ACCENT_COLORS] o "paleta" (el de la paleta). */
    var accentId by mutableStateOf("paleta")
    /** Tamaño del texto en por ciento. */
    var escala by mutableStateOf(100)

    val tones: Tones get() = (if (Modo.oscuro) palette.dark else palette.light).let { b ->
        ACCENT_COLORS.firstOrNull { it.id == accentId }?.let { a -> b.copy(general = (if (Modo.oscuro) a.dark else a.light) or 0xFF000000) } ?: b
    }

    private val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )
    private val cache = HashMap<String, FontFamily>()

    /** La letra de Google Fonts por su nombre (con sus gruesos reales), o null si es una de las que van dentro de la app. */
    fun family(name: String): FontFamily? {
        if (name.isBlank() || name == "Atrio") return null
        if (name == "Vitrina") return VitrinaDisplay
        return cache.getOrPut(name) {
            val c = (DISPLAY_FONTS + BODY_FONTS).firstOrNull { it.name == name }
            val ws = c?.weights?.takeIf { it.isNotEmpty() } ?: intArrayOf(400, 700)
            FontFamily(ws.map { GFont(GoogleFont(name), provider, FontWeight(it)) })
        }
    }

    fun aplicar(s: SkinStyle) {
        palette = PALETTES.firstOrNull { it.id == s.palette } ?: palette
        display = s.display; body = s.body
        forma = FORMAS.firstOrNull { it.id == s.form } ?: forma
    }

    /** El estilo listo que coincide con lo elegido ahora (si es uno de la lista). */
    fun estiloActual(): SkinStyle? = STYLES.firstOrNull { it.palette == palette.id && it.display == display && it.body == body && it.form == forma.id }

    private fun p(c: Context) = c.getSharedPreferences("portico_tema", Context.MODE_PRIVATE)

    fun cargar(c: Context) {
        val p = p(c)
        palette = PALETTES.firstOrNull { it.id == p.getString("paleta", "vitrina") } ?: palette
        display = p.getString("display", "Vitrina") ?: "Vitrina"
        body = p.getString("body", "Atrio") ?: "Atrio"
        forma = FORMAS.firstOrNull { it.id == p.getString("forma", "vitrina") } ?: forma
        accentId = p.getString("acento", "paleta")?.takeIf { id -> id == "paleta" || ACCENT_COLORS.any { it.id == id } } ?: "paleta"
        escala = p.getInt("escala", 100).coerceIn(80, 140)
        Modo.eleccion = p.getInt("modo", 2).coerceIn(0, 2)
    }

    fun guardar(c: Context) {
        p(c).edit().putString("paleta", palette.id).putString("display", display).putString("body", body).putString("forma", forma.id)
            .putString("acento", accentId).putInt("escala", escala).putInt("modo", Modo.eleccion).apply()
    }
}

// ───── colores (los mismos nombres que en Atrio) ─────
val Paper: Color get() = Skin.tones.cPaper
val Crudo: Color get() = Skin.tones.cCrudo
val Sand: Color get() = Skin.tones.cSand
val Line: Color get() = Skin.tones.cLine
val Charcoal: Color get() = Skin.tones.cInk
val Edge: Color get() = Skin.tones.cEdge
val Muted: Color get() = Skin.tones.cMuted
/** El acento (el coral de Vitrina). */
val Acento: Color get() = Color(Skin.tones.general)
val Dorado: Color get() = Color(Skin.tones.gold)
/** Texto sobre el acento: blanco o tinta oscura, el que se lea. */
val SobreAcento: Color get() = if (Acento.luminance2() > 0.5f) Color(0xFF14151A) else Color.White
fun Color.luminance2(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

fun appColors() = if (Modo.oscuro) darkColorScheme(
    primary = Charcoal, onPrimary = Crudo, secondary = Acento, onSecondary = SobreAcento,
    background = Paper, onBackground = Charcoal, surface = Crudo, onSurface = Charcoal,
    surfaceVariant = Sand, onSurfaceVariant = Charcoal, surfaceContainer = Crudo, surfaceContainerHigh = Crudo,
    surfaceContainerHighest = Sand, surfaceContainerLow = Crudo, outline = Edge, outlineVariant = Line, error = Acento
) else lightColorScheme(
    primary = Charcoal, onPrimary = Crudo, secondary = Acento, onSecondary = SobreAcento,
    background = Paper, onBackground = Charcoal, surface = Crudo, onSurface = Charcoal,
    surfaceVariant = Sand, onSurfaceVariant = Charcoal, surfaceContainer = Crudo, surfaceContainerHigh = Crudo,
    surfaceContainerHighest = Sand, surfaceContainerLow = Crudo, outline = Edge, outlineVariant = Line, error = Acento
)

// ───── letras ─────
val AtrioDisplay = FontFamily(
    Font(R.font.display_regular, FontWeight.Normal), Font(R.font.display_medium, FontWeight.Medium),
    Font(R.font.display_semibold, FontWeight.SemiBold), Font(R.font.display_semibold, FontWeight.Bold),
    Font(R.font.display_italic, FontWeight.Normal, FontStyle.Italic)
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val VitrinaDisplay = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(R.font.bricolage, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.width(if (w >= 700) 92f else 100f)))
    }
)

val AtrioBody = FontFamily(
    Font(R.font.body_regular, FontWeight.Normal), Font(R.font.body_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.body_medium, FontWeight.Medium), Font(R.font.body_semibold, FontWeight.SemiBold),
    Font(R.font.body_bold, FontWeight.Bold), Font(R.font.body_bold, FontWeight.ExtraBold)
)

val Display: FontFamily get() = Skin.family(Skin.display) ?: AtrioDisplay
val Body: FontFamily get() = Skin.family(Skin.body) ?: AtrioBody

private val base = Typography()
private fun TextStyle.body() = copy(fontFamily = Body)
private fun TextStyle.display() = copy(fontFamily = Display, fontWeight = FontWeight.Normal)

val AppTypography: Typography get() = Typography(
    displayLarge = base.displayLarge.display(), displayMedium = base.displayMedium.display(), displaySmall = base.displaySmall.display(),
    headlineLarge = base.headlineLarge.display(), headlineMedium = base.headlineMedium.display(), headlineSmall = base.headlineSmall.display(),
    titleLarge = base.titleLarge.display(), titleMedium = base.titleMedium.body(), titleSmall = base.titleSmall.body(),
    bodyLarge = base.bodyLarge.body().copy(fontSize = 15.sp, lineHeight = 22.sp), bodyMedium = base.bodyMedium.body(), bodySmall = base.bodySmall.body(),
    labelLarge = base.labelLarge.body().copy(fontWeight = FontWeight.SemiBold), labelMedium = base.labelMedium.body().copy(fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.body()
)

/** La raíz del tema: modo claro/oscuro, colores, letras y tamaño del texto. */
@Composable
fun PorticoTheme(content: @Composable () -> Unit) {
    val sistema = isSystemInDarkTheme()
    Modo.oscuro = when (Modo.eleccion) { 1 -> false; 2 -> true; else -> sistema }
    val d = LocalDensity.current
    androidx.compose.runtime.CompositionLocalProvider(
        LocalDensity provides Density(d.density, d.fontScale * Skin.escala / 100f)
    ) {
        MaterialTheme(colorScheme = appColors(), typography = AppTypography, content = content)
    }
}
