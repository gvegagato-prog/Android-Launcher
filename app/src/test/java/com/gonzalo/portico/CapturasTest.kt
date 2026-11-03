package com.gonzalo.portico

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Capturas de la interfaz en el ordenador (sin emulador), con juegos de ejemplo. Para revisar el diseño:
 *   gradle :app:testDebugUnitTest --tests '*CapturasTest*' -Proborazzi.test.record=true
 * Las imágenes quedan en app/build/outputs/roborazzi/.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class CapturasTest {
    @get:Rule val rule = createComposeRule()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    private fun siembra() {
        Catalogo.cargar(ctx); Skin.cargar(ctx); Ajustes.cargar(ctx)
        Ajustes.portadasLibretro = false
        Biblioteca.cargada = true
        Biblioteca.juegos.clear()
        val ahora = System.currentTimeMillis()
        fun rom(n: String, s: String, k: Int, fav: Boolean = false, jug: Long = 0, ms: Long = 0, veces: Int = 0) =
            Biblioteca.juegos.add(Juego("r$k", n, Tipo.ROM, sistema = s, uri = "content://x/$k", archivo = n, favorito = fav, anadido = ahora - k * 1000L, ultima = jug, msJugados = ms, veces = veces))
        rom("Pokémon Esmeralda", "gba", 1, fav = true, jug = ahora - 3600_000, ms = 3 * 3600_000L + 1_200_000, veces = 7)
        Biblioteca.juegos[0] = Biblioteca.juegos[0].copy(terminado = true, finalizado = ahora - 86_400_000L * 3, nota = 8.5f, notas = "Me falta el Frente de Batalla. Ojo: guardar antes de la Liga.")
        rom("Metroid Fusion", "gba", 2); rom("Advance Wars", "gba", 3, fav = true); rom("The Minish Cap", "gba", 4)
        rom("Pokémon Platino", "nds", 5, jug = ahora - 86_400_000L * 2, ms = 12 * 3600_000L, veces = 21); rom("Mario Kart DS", "nds", 6); rom("Hollow Knight (SNES hack)", "snes", 7)
        Biblioteca.juegos.add(Juego("a1", "Stardew Valley", Tipo.APP, paquete = "com.chucklefish.stardewvalley", anadido = ahora - 9000))
        Biblioteca.juegos.add(Juego("w1", "PokéRogue", Tipo.WEB, url = "https://pokerogue.net", anadido = ahora - 10000, ultima = ahora - 600_000, veces = 3, msJugados = 5400_000))
    }

    private fun foto(nombre: String) {
        rule.waitForIdle(); Thread.sleep(500); rule.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("build/outputs/roborazzi/$nombre.png")
    }

    private fun montar(oscuro: Boolean = true) {
        coil.Coil.setImageLoader(coil.ImageLoader.Builder(ctx).dispatcher(Dispatchers.Unconfined).allowHardware(false).build())
        Modo.eleccion = if (oscuro) 2 else 1
        rule.setContent { PorticoTheme { Surface(Modifier.fillMaxSize(), color = Paper) { PorticoApp() } } }
    }

    private fun ir(texto: String) { rule.onNodeWithText(texto).performScrollTo().performClick() }

    @Test
    fun interfaces() {
        org.junit.Assume.assumeTrue(System.getProperty("roborazzi.test.record") == "true")
        Catalogo.cargar(ctx); Skin.cargar(ctx)
        Biblioteca.cargada = true; Biblioteca.juegos.clear()
        montar(oscuro = false)
        foto("1_portada_vacia")
    }

    @Test
    fun conJuegos() {
        org.junit.Assume.assumeTrue(System.getProperty("roborazzi.test.record") == "true")
        siembra()
        montar(oscuro = false)
        foto("2_portada")
        ir("JUEGOS"); foto("3_juegos")
        ir("← PÓRTICO"); ir("SISTEMAS"); foto("4_sistemas")
        ir("← PÓRTICO"); rule.onNodeWithContentDescription("Ajustes").performClick(); foto("5_ajustes")
        ir("← PÓRTICO"); ir("JUEGOS")
        rule.onAllNodesWithText("Pokémon Esmeralda", substring = true, useUnmergedTree = true)[0].performClick(); foto("6_ficha")
    }

    @Test
    fun oscuro() {
        org.junit.Assume.assumeTrue(System.getProperty("roborazzi.test.record") == "true")
        siembra()
        montar(oscuro = true)
        foto("7_portada_oscuro")
        ir("JUEGOS"); foto("8_juegos_oscuro")
    }

    @Test
    fun hojaAnadir() {
        org.junit.Assume.assumeTrue(System.getProperty("roborazzi.test.record") == "true")
        siembra()
        montar(oscuro = false)
        ir("AÑADIR"); foto("9_anadir")
    }

    @Test
    fun fichaYSusCasillas() {
        org.junit.Assume.assumeTrue(System.getProperty("roborazzi.test.record") == "true")
        siembra()
        montar(oscuro = false)
        ir("JUEGOS")
        rule.onAllNodesWithText("Pokémon Esmeralda", substring = true, useUnmergedTree = true)[0].performClick()
        rule.onNodeWithText("HORAS JUGADAS").performScrollTo(); foto("12_ficha_casillas")
        rule.onNodeWithText("EDITAR").performClick()
        rule.onNodeWithText("FILA 1").performScrollTo(); foto("13_ficha_editando")
        rule.onNodeWithText("RESTABLECER EL DISEÑO DE SIEMPRE").performScrollTo(); foto("14_ficha_editando_abajo")
    }

    @Test
    fun buscadorDePortadas() {
        org.junit.Assume.assumeTrue(System.getProperty("roborazzi.test.record") == "true")
        siembra()
        montar(oscuro = false)
        ir("JUEGOS")
        rule.onAllNodesWithText("Pokémon Esmeralda", substring = true, useUnmergedTree = true)[0].performClick()
        rule.onNodeWithText("PORTADA\u00A0→").performScrollTo().performClick(); foto("10_portada_opciones")
        rule.onNodeWithText("Buscar en internet\u00A0→").performClick(); foto("11_buscador")
    }
}
