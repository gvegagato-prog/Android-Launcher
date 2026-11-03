package com.gonzalo.portico

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Los widgets, dibujados en el ordenador con juegos de ejemplo (build/outputs/widgets/) y probados con distintos temas. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class WidgetsTest {
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    private fun portada(nombre: String, a: Int, b: Int): String {
        val f = File(ctx.cacheDir, "c_${nombre.hashCode()}.png")
        if (f.exists()) return f.absolutePath
        val bmp = Bitmap.createBitmap(300, 450, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawRect(0f, 0f, 300f, 450f, Paint().apply { shader = LinearGradient(0f, 0f, 300f, 450f, a, b, Shader.TileMode.CLAMP) })
        c.drawText(nombre.take(1), 30f, 160f, Paint().apply { color = 0x55FFFFFF; textSize = 120f; isAntiAlias = true; isFakeBoldText = true })
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return f.absolutePath
    }

    private fun siembra(vacia: Boolean = false) {
        Catalogo.cargar(ctx); Ajustes.cargar(ctx); Ajustes.portadasLibretro = false
        Biblioteca.cargada = true; Biblioteca.juegos.clear()
        if (vacia) return
        val ahora = System.currentTimeMillis()
        Biblioteca.juegos.addAll(listOf(
            Juego("1", "Pokémon Esmeralda", Tipo.ROM, sistema = "gba", archivo = "x", portada = portada("Pokemon", 0xFF2E9E6B.toInt(), 0xFF0B3D2E.toInt()), anadido = ahora - 5, ultima = ahora - 3600_000, veces = 7, msJugados = 3 * 3600_000L + 1_200_000),
            Juego("2", "Metroid Fusion", Tipo.ROM, sistema = "gba", archivo = "y", portada = portada("Metroid", 0xFFE0603A.toInt(), 0xFF5A1D0A.toInt()), anadido = ahora - 4, ultima = ahora - 86_400_000L, veces = 2, msJugados = 5400_000L),
            Juego("3", "Advance Wars", Tipo.ROM, sistema = "gba", archivo = "z", portada = portada("Advance", 0xFF6C4DD6.toInt(), 0xFF2A1A66.toInt()), anadido = ahora - 3, ultima = ahora - 2 * 86_400_000L, veces = 1, msJugados = 1800_000L),
            Juego("4", "PokéRogue", Tipo.WEB, url = "https://pokerogue.net", anadido = ahora - 2, ultima = ahora - 3 * 86_400_000L, veces = 3, msJugados = 7200_000L),
            Juego("5", "Hollow Knight", Tipo.ROM, sistema = "snes", archivo = "w", portada = portada("Hollow", 0xFF3C7DD9.toInt(), 0xFF10264A.toInt()), anadido = ahora - 1, terminado = true, nota = 9f),
        ))
    }

    private fun foto(nombre: String, tipo: WTipo, w: Float, h: Float, noche: Boolean) {
        val pag = runBlocking { Widgets.componer(ctx, tipo, 1, w, h) }
        val bm = pag.bitmap(ctx, w, h, noche, 2f)
        File("build/outputs/widgets").mkdirs()
        File("build/outputs/widgets/$nombre.png").outputStream().use { bm.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val tamanos = mapOf(WTipo.JUGANDO to (264f to 132f), WTipo.ESTANTE to (280f to 165f), WTipo.PORTADA to (264f to 200f), WTipo.SIGUIENTE to (264f to 132f), WTipo.MARCA to (256f to 64f))

    @Test fun dibujaTodosSinFallar() {
        // (no necesita la marca de grabar: comprueba que cada widget se compone y se dibuja con y sin juegos)
        for (vacia in listOf(false, true)) {
            siembra(vacia)
            Skin.origenAtrio = false; Skin.invertido = false
            for ((t, s) in tamanos) for (noche in listOf(false, true)) {
                val pag = runBlocking { Widgets.componer(ctx, t, 1, s.first, s.second) }
                val bm = pag.bitmap(ctx, s.first, s.second, noche, 1f)
                assertTrue("$t sale con tamaño", bm.width > 0 && bm.height > 0)
            }
        }
    }

    @Test fun elTemaSeNotaEnElPapel() {
        siembra()
        Skin.origenAtrio = false; Skin.invertido = false
        val h = Hoja(ctx, 100f, 100f, false, 1f); val claro = h.papel
        val o = Hoja(ctx, 100f, 100f, true, 1f).papel
        assertNotEquals("claro y oscuro tienen papeles distintos", claro, o)
        Skin.invertido = true
        assertEquals("invertido: el papel claro pasa a ser la tinta", h.tinta, Hoja(ctx, 100f, 100f, false, 1f).papel)
        // con los colores de Atrio (fijos), claro y oscuro son iguales y el acento es el de Atrio
        TemaAtrio.tonos = TemaAtrio.tonosDe(listOf(0xFFF3EEE4.toInt(), 0xFFFAF6EE.toInt(), 0xFF15120F.toInt(), 0xFF2F5D8A.toInt(), 0xFFE9E2D5.toInt(), 0xFFD9D0C1.toInt(), 0xFFBDB2A0.toInt(), 0xFF7C746A.toInt(), 0xFFB8861B.toInt()))
        Skin.origenAtrio = true; Skin.invertido = true
        val a = Hoja(ctx, 100f, 100f, false, 1f)
        assertEquals(0xFF15120F.toInt(), a.papel); assertEquals(0xFFF3EEE4.toInt(), a.tinta); assertEquals(0xFF2F5D8A.toInt(), a.acento)
        assertEquals(a.papel, Hoja(ctx, 100f, 100f, true, 1f).papel)
        assertNull("con colores fijos se dibuja un solo widget", if (Skin.coloresFijos) null else true)
        assertEquals(false, Widgets.noche())
    }

    @Test fun dibujaLasVistasPrevias() {
        org.junit.Assume.assumeTrue(System.getProperty("roborazzi.test.record") == "true")
        siembra()
        Skin.origenAtrio = false; Skin.invertido = false
        for ((t, s) in tamanos) foto("w_prev_${t.name.lowercase()}", t, s.first, s.second, false)
        // y con otros temas, para verlos
        Skin.invertido = true
        for ((t, s) in tamanos) foto("inv_${t.name.lowercase()}", t, s.first, s.second, false)
        TemaAtrio.tonos = TemaAtrio.tonosDe(listOf(0xFFF3EEE4.toInt(), 0xFFFAF6EE.toInt(), 0xFF15120F.toInt(), 0xFF2F5D8A.toInt(), 0xFFE9E2D5.toInt(), 0xFFD9D0C1.toInt(), 0xFFBDB2A0.toInt(), 0xFF7C746A.toInt(), 0xFFB8861B.toInt()))
        Skin.origenAtrio = true
        for ((t, s) in tamanos) foto("atrio_inv_${t.name.lowercase()}", t, s.first, s.second, false)
        Skin.invertido = false
        for ((t, s) in tamanos) foto("atrio_${t.name.lowercase()}", t, s.first, s.second, false)
        siembra(vacia = true)
        foto("vacio_jugando", WTipo.JUGANDO, 264f, 132f, false)
    }
}
