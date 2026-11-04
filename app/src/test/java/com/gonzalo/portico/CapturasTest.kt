package com.gonzalo.portico

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onLast
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
        // (lo que otra prueba haya dejado puesto en la apariencia)
        Skin.invertido = false; Skin.origenAtrio = false; Skin.fondoId = ""
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
        // Los estados: lo pasado, lo que se está jugando, lo pendiente y lo abandonado
        fun estado(id: String, e: Estado) { val i = Biblioteca.juegos.indexOfFirst { it.id == id }; Biblioteca.juegos[i] = Biblioteca.juegos[i].conEstado(e, ahora - 86_400_000L * 3) }
        estado("r1", Estado.PASADO); estado("r5", Estado.JUGANDO); estado("w1", Estado.JUGANDO); estado("r4", Estado.ABANDONADO)
        // Fichas sin lanzador (de Atrio): un juego de PC pasado y uno de Switch a medias
        Biblioteca.juegos.add(nuevaFicha("Hades", "PC", anadido = ahora - 20_000).conEstado(Estado.PASADO, ahora - 86_400_000L * 10).copy(nota = 9f, msPropios = 40 * 3600_000L, msJugados = 40 * 3600_000L, alias = listOf("juego:hades")))
        Biblioteca.juegos.add(nuevaFicha("Tears of the Kingdom", "Switch", anadido = ahora - 30_000).conEstado(Estado.JUGANDO).copy(msPropios = 25 * 3600_000L, msJugados = 25 * 3600_000L, ultima = ahora - 7200_000))
        // Dos sagas
        Sagas.cargada = true; Sagas.lista.clear(); Sagas.borradas.clear()
        Sagas.lista.add(Saga("s1", "Pokémon", listOf(Biblioteca.por("r1")!!.clave, Biblioteca.por("r5")!!.clave), 1))
        Sagas.lista.add(Saga("s2", "The Legend of Zelda", listOf(Biblioteca.por("r4")!!.clave, Biblioteca.juegos.first { it.titulo == "Tears of the Kingdom" }.clave), 1))
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
    /** Toca el nodo con ese texto que está dentro de la pantalla (el pager tiene compuestas las páginas vecinas, fuera de la vista). */
    private fun tocarVisible(texto: String) {
        val nodos = rule.onAllNodesWithText(texto, substring = true).fetchSemanticsNodes()
        val ancho = rule.onRoot().fetchSemanticsNode().size.width
        val i = nodos.indexOfFirst { it.boundsInRoot.left >= 0f && it.boundsInRoot.right <= ancho }
        rule.onAllNodesWithText(texto, substring = true)[if (i < 0) 0 else i].performClick(); rule.waitForIdle()
    }
    /** Un enlace de la barra de arriba (no hace falta desplazarse hasta él). */
    private fun pulsar(texto: String) { rule.onNodeWithText(texto).performClick(); rule.waitForIdle() }

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
        ir("JUGANDO"); foto("3_juegos")
        rule.onAllNodesWithText("PENDIENTE")[0].performClick(); rule.waitForIdle(); foto("3b_pendiente")
        rule.onAllNodesWithText("PASADO")[0].performClick(); rule.waitForIdle(); foto("3c_pasado")
        tocarVisible("EMULACIÓN"); foto("3d_pasado_emulacion")
        rule.onAllNodesWithText("SAGAS")[0].performClick(); rule.waitForIdle(); foto("3e_sagas")
        rule.onAllNodesWithText("The Legend of Zelda", useUnmergedTree = true).onLast().performClick(); rule.waitForIdle(); foto("3f_saga_abierta")
        rule.onNodeWithText("VER LA SAGA →", useUnmergedTree = true).performClick(); foto("3g_pagina_saga")
        pulsar("← VOLVER")
        pulsar("← CARMESÍ"); rule.onNodeWithContentDescription("Ajustes").performClick(); foto("5_ajustes")
        rule.onNodeWithText("Sistemas y emuladores", substring = true).performScrollTo().performClick(); foto("4_sistemas")
        pulsar("← CARMESÍ"); pulsar("← CARMESÍ"); ir("PASADO")
        rule.onAllNodesWithText("Pokémon Esmeralda", substring = true, useUnmergedTree = true)[0].performClick(); foto("6_ficha")
        pulsar("← VOLVER")
        rule.onAllNodesWithText("Hades", substring = true, useUnmergedTree = true)[0].performClick(); foto("6b_ficha_sin_lanzador")
    }

    @Test
    fun tuPc() {
        org.junit.Assume.assumeTrue(System.getProperty("roborazzi.test.record") == "true")
        siembra()
        val ahora = System.currentTimeMillis()
        fun g(k: String, n: String, m: Int = 0, lp: Long = 0) = org.json.JSONObject().put("k", k).put("n", n).put("m", m).put("lp", lp).put("c", "")
        val arbol = org.json.JSONObject()
            .put("live", org.json.JSONObject().put("pc1", org.json.JSONObject().put("app", "atrio").put("dn", "Torre").put("os", "win32").put("on", true).put("at", ahora).put("today", 95)
                .put("game", org.json.JSONObject().put("key", "steam:1145360").put("name", "Hades").put("s", ahora - 47 * 60_000L).put("sg", 7L))
                .put("st", org.json.JSONObject().put("on", true).put("run", true).put("name", "torre"))))
            .put("lib", org.json.JSONObject().put("pc1", org.json.JSONObject().put("name", "torre").put("games", org.json.JSONArray()
                .put(g("steam:1145360", "Hades", 2400, ahora - 47 * 60_000L)).put(g("steam:504230", "Celeste", 600, ahora - 86_400_000L * 4)).put(g("epic:aw2", "Alan Wake 2"))
                .put(g("steam:1245620", "Elden Ring", 5000, ahora - 86_400_000L * 40)).put(g("steam:413150", "Stardew Valley", 90, ahora - 86_400_000L * 9)).put(g("gog:witcher3", "The Witcher 3")))))
        Suite.simular(arbol)
        JuegosPc.aplicar(ctx, Suite.leer(arbol, "yo"), emptyList())
        montar(oscuro = false)
        foto("22_portada_pc")
        rule.onAllNodesWithText("TU PC")[0].performClick(); rule.waitForIdle(); foto("23_tu_pc")
        rule.onAllNodesWithText("BIBLIOTECA", substring = true)[0].performClick(); rule.waitForIdle(); foto("24_tu_pc_biblioteca")
        rule.onAllNodesWithText("EQUIPO", substring = true)[0].performClick(); rule.waitForIdle(); foto("25_tu_pc_equipo")
        rule.onAllNodesWithText("AHORA", substring = true)[0].performClick(); rule.waitForIdle()
        rule.onAllNodesWithText("FICHA →", substring = true)[0].performClick(); rule.waitForIdle(); foto("26_ficha_pc")
        rule.onNodeWithText("EN AZABACHE").performScrollTo(); foto("26b_ficha_pc_abajo")
    }

    @Test
    fun inicio() {
        org.junit.Assume.assumeTrue(System.getProperty("roborazzi.test.record") == "true")
        siembra()
        Inicio.poner(ConfigInicio(fijados = listOf(Biblioteca.por("r3")!!.clave, Biblioteca.por("r6")!!.clave, Biblioteca.juegos.first { it.titulo == "Hades" }.clave)), 1)
        montar(oscuro = false)
        rule.waitForIdle(); Thread.sleep(2500); foto("20_inicio")
        rule.onNodeWithText("MIS ELEGIDOS").performScrollTo(); foto("20b_inicio_estantes")
        rule.onNodeWithText("EDITAR").performScrollTo().performClick(); rule.waitForIdle(); rule.onNodeWithText("ESTANTES").performScrollTo(); foto("21_inicio_editando")
        Inicio.poner(ConfigInicio(), 2)
    }

    @Test
    fun ajustesConFondoOscuro() {
        org.junit.Assume.assumeTrue(System.getProperty("roborazzi.test.record") == "true")
        siembra(); Skin.fondoId = "granate"
        montar(oscuro = false)   // (el modo claro: lo que fija el color es el fondo, no el modo)
        rule.onNodeWithContentDescription("Ajustes").performClick(); foto("30_ajustes_fondo_oscuro")
        Skin.fondoId = ""
    }

    @Test
    fun oscuro() {
        org.junit.Assume.assumeTrue(System.getProperty("roborazzi.test.record") == "true")
        siembra()
        montar(oscuro = true)
        foto("7_portada_oscuro")
        ir("JUGANDO"); foto("8_juegos_oscuro")
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
        ir("PASADO")
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
        ir("PASADO")
        rule.onAllNodesWithText("Pokémon Esmeralda", substring = true, useUnmergedTree = true)[0].performClick()
        rule.onNodeWithText("PORTADA\u00A0→").performScrollTo().performClick(); foto("10_portada_opciones")
        rule.onNodeWithText("Buscar en internet\u00A0→").performClick(); foto("11_buscador")
    }

    @Test
    fun invertidos() {
        org.junit.Assume.assumeTrue(System.getProperty("roborazzi.test.record") == "true")
        siembra()
        Skin.invertido = true
        montar(oscuro = false)
        foto("15_invertido_portada")
        ir("PASADO"); foto("16_invertido_juegos")
        rule.onAllNodesWithText("Pokémon Esmeralda", substring = true, useUnmergedTree = true)[0].performClick()
        rule.onNodeWithText("HORAS JUGADAS").performScrollTo(); foto("17_invertido_ficha")
    }

    @Test
    fun invertidosEnOscuro() {
        org.junit.Assume.assumeTrue(System.getProperty("roborazzi.test.record") == "true")
        siembra()
        Skin.invertido = true
        montar(oscuro = true)
        foto("18_invertido_oscuro_portada")
    }

    @Test
    fun coloresDeAtrioInvertidos() {
        org.junit.Assume.assumeTrue(System.getProperty("roborazzi.test.record") == "true")
        siembra()
        // Un Atrio de mentira con el acento azul que el usuario habría elegido
        org.robolectric.Robolectric.setupContentProvider(TemaAtrioTest.AtrioFalso::class.java, "com.gonzalo.gamerate.tema")
        TemaAtrio.refrescar(ctx)
        Skin.origenAtrio = true; Skin.invertido = true
        montar(oscuro = false)
        foto("19_atrio_invertido_portada")
        rule.onNodeWithContentDescription("Ajustes").performClick()
        rule.onNodeWithText("COLORES DE ATRIO", substring = true).performScrollTo(); foto("20_ajustes_colores_atrio")
    }
}
