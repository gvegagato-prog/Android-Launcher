package com.gonzalo.portico

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Las secciones: los menús pequeños filtran y se quitan con otro toque; las pestañas llevan la cuenta. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class SeccionesTest {
    @get:Rule val rule = createComposeRule()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun losMenusPequenosFiltranYSeQuitanConOtroToque() {
        Catalogo.cargar(ctx); Skin.cargar(ctx); Ajustes.cargar(ctx); Ajustes.portadasLibretro = false
        Biblioteca.cargada = true; Biblioteca.juegos.clear(); Sagas.cargada = true; Sagas.lista.clear()
        Biblioteca.juegos.add(Juego("r1", "Pokémon Esmeralda", Tipo.ROM, sistema = "gba", uri = "content://x", archivo = "pe", anadido = 1).conEstado(Estado.PASADO, 5))
        Biblioteca.juegos.add(nuevaFicha("Hades", "PC", anadido = 2).conEstado(Estado.PASADO, 6))
        Biblioteca.juegos.add(Juego("a1", "Stardew Valley", Tipo.APP, paquete = "x.y", anadido = 3))
        rule.setContent { PorticoTheme { Surface(Modifier.fillMaxSize(), color = Paper) { PantSecciones(Destino.Pasado, {}, {}, {}, {}, {}) } } }
        rule.onNodeWithText("Hades", useUnmergedTree = true).assertIsDisplayed()
        // Solo la página «Pasado» está en pantalla; sus menús pequeños son los que se tocan
        fun tocar(t: String) { val n = rule.onAllNodesWithText(t, substring = true); n[n.fetchSemanticsNodes().indexOfFirst { it.boundsInRoot.left >= 0f && it.boundsInRoot.right <= 1080f }].performClick(); rule.waitForIdle() }
        tocar("EMULACIÓN")
        assertTrue("con Emulación, Hades (PC) no sale", rule.onAllNodesWithText("Hades", useUnmergedTree = true).fetchSemanticsNodes().none { it.boundsInRoot.left >= 0f && it.boundsInRoot.right <= 1080f && it.boundsInRoot.top >= 0f })
        rule.onNodeWithText("Pokémon Esmeralda", useUnmergedTree = true).assertIsDisplayed()
        tocar("EMULACIÓN")
        rule.onNodeWithText("Hades", useUnmergedTree = true).assertIsDisplayed()
        Biblioteca.juegos.clear(); Sagas.lista.clear()
    }
}
