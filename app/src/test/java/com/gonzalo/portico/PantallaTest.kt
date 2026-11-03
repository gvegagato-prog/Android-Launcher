package com.gonzalo.portico

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** La pantalla de inicio como lienzo: colocar, mezclar entre móviles y las pantallas de verdad. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class PantallaTest {
    @get:Rule val rule = createComposeRule()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before fun limpia() {
        Pantalla.items.clear(); Pantalla.borrados.clear()
        Biblioteca.cargada = true; Biblioteca.juegos.clear(); Biblioteca.borrados.clear()
    }

    private fun e(id: String, x: Int, y: Int, w: Int = 1, h: Int = 1, mod: Long = 1, color: Int = 0, ref: String = "web:a") = Elemento(id, Elemento.JUEGO, ref, x, y, w, h, color, mod)

    @Test fun huecosYColisiones() {
        Pantalla.items.addAll(listOf(e("a", 0, 0, 2, 1), e("b", 2, 0, 2, 2)))
        assertEquals(0 to 1, Pantalla.hueco(2, 1)); assertEquals(0 to 1, Pantalla.hueco(1, 1))
        assertEquals(0 to 2, Pantalla.hueco(4, 1))
        assertTrue(Pantalla.libre(e("a", 0, 0, 2, 2).copy(w = 2, h = 1)))
        assertFalse("pisa a b", Pantalla.libre(e("a", 1, 0, 2, 1)))
        assertFalse("se sale por la derecha", Pantalla.libre(e("a", 3, 2, 2, 1)))
        assertFalse("se sale por la izquierda", Pantalla.libre(e("a", -1, 2, 1, 1)))
        assertTrue(Pantalla.libre(e("c", 0, 1, 2, 1)))
    }

    @Test fun mezclaPorElementoGanaElMasReciente() {
        val loc = listOf(e("a", 0, 0, mod = 100), e("b", 1, 0, mod = 100, color = 5))
        val rem = listOf(e("a", 3, 3, mod = 200), e("b", 0, 1, mod = 50), e("c", 2, 2, mod = 10)) to mapOf("z" to 5L)
        val r = MezclaPantalla.mezclar(loc, emptyMap(), listOf(rem), ahora = 1000)
        assertEquals(3, r.items.size)
        assertEquals(3 to 3, r.items.first { it.id == "a" }.let { it.x to it.y })        // el remoto era más nuevo
        assertEquals(5, r.items.first { it.id == "b" }.color)                              // el local era más nuevo
        assertTrue(r.cambio); assertEquals(5L, r.borrados["z"])
        // quitar en un móvil quita en el otro; pero un cambio posterior al borrado lo mantiene
        val q = MezclaPantalla.mezclar(loc, emptyMap(), listOf(emptyList<Elemento>() to mapOf("a" to 150L)), ahora = 1000)
        assertEquals(listOf("a" to false).size, 1); assertEquals(setOf("a", "b").minus("a"), q.items.map { it.id }.toSet())
        val q2 = MezclaPantalla.mezclar(listOf(e("a", 0, 0, mod = 300)), emptyMap(), listOf(emptyList<Elemento>() to mapOf("a" to 150L)), ahora = 1000)
        assertEquals(1, q2.items.size)
        assertFalse(MezclaPantalla.mezclar(r.items, r.borrados, listOf(rem), ahora = 1000).cambio)
    }

    @Test fun viajaEnLaCopiaDelDispositivo() {
        val b = Mezcla.deJson(Mezcla.aJson(Mezcla.aBlob("A", emptyList(), emptyMap(), listOf(e("a", 1, 2, 2, 3, color = 0xFFC8362B.toInt())), mapOf("x" to 9L))))!!
        assertEquals(1, b.pantalla.size); assertEquals(3, b.pantalla[0].h); assertEquals(0xFFC8362B.toInt(), b.pantalla[0].color); assertEquals(9L, b.pBorrados["x"])
        // Una copia vieja (sin pantalla) sigue leyéndose
        assertTrue(Mezcla.deJson("""{"v":1,"dev":"B","juegos":[],"borrados":{}}""")!!.pantalla.isEmpty())
    }

    @Test fun laPrimeraVezSeRellenaConLosRecientes() {
        Biblioteca.juegos.add(Juego("1", "Zelda", Tipo.ROM, sistema = "gba", archivo = "Zelda", uri = "content://x", ultima = 5))
        Biblioteca.juegos.add(Juego("2", "Metroid", Tipo.ROM, sistema = "gba", archivo = "Metroid", uri = "content://y", ultima = 9))
        Pantalla.cargar(ctx); Pantalla.inicial(ctx)
        assertEquals(2, Pantalla.items.size); assertEquals(Biblioteca.juegos[1].clave, Pantalla.items[0].ref)   // el más reciente primero
        assertTrue(Pantalla.items.all { it.w == 2 && it.h == 1 })
        Pantalla.inicial(ctx); assertEquals("solo una vez", 2, Pantalla.items.size)
    }

    @Test fun laPantallaDeInicioMuestraElMenuYElLienzo() {
        Catalogo.cargar(ctx); Skin.cargar(ctx)
        Biblioteca.juegos.add(Juego("1", "Zelda de prueba", Tipo.ROM, sistema = "gba", archivo = "Zelda", uri = "content://x"))
        Pantalla.items.add(Elemento("e1", Elemento.JUEGO, Biblioteca.juegos[0].clave, 0, 0, 2, 1))
        var ir: Destino? = null
        rule.setContent { PorticoTheme { Surface(Modifier.fillMaxSize(), color = Paper) { PantInicio({}, {}, {}, { ir = it }, {}) } } }
        rule.onNodeWithTag("el-e1").assertExists()
        listOf("RECIENTES", "FAVORITOS", "ROMS", "ANDROID", "WEB", "TODO", "SISTEMAS").forEach { rule.onNodeWithText(it).assertExists() }
        rule.onNodeWithText("ROMS").performClick(); assertEquals(Destino.Roms, ir)
        // Editar: aparecen las instrucciones y el botón de añadir
        rule.onNodeWithText("EDITAR").performClick()
        rule.onNodeWithText("+ AÑADIR").assertExists(); rule.onNodeWithText("LISTO").assertExists()
        rule.onNodeWithTag("el-e1").performTouchInput { click(center) }   // toca el elemento: diálogo de color
        rule.onNodeWithText("Color de fondo".uppercase()).assertExists()
        rule.onNodeWithText("QUITAR").performClick()
        assertTrue(Pantalla.items.isEmpty()); assertTrue(Pantalla.borrados.containsKey("e1"))
    }

    private fun montarEditando(vararg es: Elemento) {
        Catalogo.cargar(ctx); Skin.cargar(ctx)
        Biblioteca.juegos.add(Juego("1", "Zelda de prueba", Tipo.ROM, sistema = "gba", archivo = "Zelda", uri = "content://x"))
        es.forEach { Pantalla.items.add(it.copy(ref = Biblioteca.juegos[0].clave)) }
        rule.setContent { PorticoTheme { Surface(Modifier.fillMaxSize(), color = Paper) { PantInicio({}, {}, {}, {}, {}) } } }
        rule.onNodeWithText("EDITAR").performClick()
    }
    private val celdaPx get() = (411f - 40f) / Pantalla.COLUMNAS * 3f

    @Test fun arrastrarMueveYSeEncajaEnLasCasillas() {
        montarEditando(e("e1", 0, 0, 1, 1))
        rule.onNodeWithTag("el-e1").performTouchInput { down(center); moveBy(androidx.compose.ui.geometry.Offset(celdaPx * 2.1f, celdaPx * 1.1f)); up() }
        rule.waitForIdle()
        assertEquals(2 to 1, Pantalla.items[0].let { it.x to it.y })
        assertTrue("cambió su hora de modificación", Pantalla.items[0].mod > 1)
    }

    @Test fun arrastrarSobreOtroLoEmpujaHaciaAbajo() {
        montarEditando(e("e1", 0, 0, 1, 1), e("e2", 1, 0, 1, 1))
        rule.onNodeWithTag("el-e1").performTouchInput { down(center); moveBy(androidx.compose.ui.geometry.Offset(celdaPx, 0f)); up() }
        rule.waitForIdle()
        assertEquals(1 to 0, Pantalla.items.first { it.id == "e1" }.let { it.x to it.y })
        assertEquals(1 to 1, Pantalla.items.first { it.id == "e2" }.let { it.x to it.y })
        assertTrue(Pantalla.items.none { a -> Pantalla.items.any { b -> a.id != b.id && a.cabe(b) } })
    }

    @Test fun agrandarConVecinosLosEmpuja() {
        montarEditando(e("e1", 0, 0, 1, 1), e("e2", 0, 1, 1, 1), e("e3", 0, 2, 1, 1))
        rule.onNodeWithTag("asa-e1").performTouchInput { down(center); moveBy(androidx.compose.ui.geometry.Offset(celdaPx * 1.05f, celdaPx * 1.05f)); up() }
        rule.waitForIdle()
        assertEquals(2 to 2, Pantalla.items.first { it.id == "e1" }.let { it.w to it.h })
        assertEquals(2, Pantalla.items.first { it.id == "e2" }.y); assertEquals(3, Pantalla.items.first { it.id == "e3" }.y)
        assertTrue("sin solapes", Pantalla.items.none { a -> Pantalla.items.any { b -> a.id != b.id && a.cabe(b) } })
    }

    @Test fun elTamanoFuncionConUnDedoRealConVariosPasos() {
        montarEditando(e("e1", 0, 0, 1, 1), e("e2", 0, 1, 1, 1))
        rule.onNodeWithTag("asa-e1").performTouchInput {
            down(center); repeat(12) { moveBy(androidx.compose.ui.geometry.Offset(celdaPx * 1.1f / 12, celdaPx * 1.1f / 12)) }; up()
        }
        rule.waitForIdle()
        assertEquals(2 to 2, Pantalla.items.first { it.id == "e1" }.let { it.w to it.h })
    }

    @Test fun elCirculoCambiaElTamano() {
        montarEditando(e("e1", 0, 0, 1, 1))
        rule.onNodeWithTag("asa-e1").performTouchInput { down(center); moveBy(androidx.compose.ui.geometry.Offset(celdaPx * 1.05f, celdaPx * 1.05f)); up() }
        rule.waitForIdle()
        assertEquals(2 to 2, Pantalla.items[0].let { it.w to it.h })
        // no se sale de las 4 columnas
        rule.onNodeWithTag("asa-e1").performTouchInput { down(center); moveBy(androidx.compose.ui.geometry.Offset(celdaPx * 9f, 0f)); up() }
        rule.waitForIdle()
        assertEquals(4, Pantalla.items[0].w)
    }

    @Test fun moverConUnDedoRealConVariosPasos() {
        montarEditando(e("e1", 0, 0, 1, 1))
        rule.onNodeWithTag("el-e1").performTouchInput {
            down(center); repeat(15) { moveBy(androidx.compose.ui.geometry.Offset(celdaPx * 2f / 15, celdaPx * 1f / 15)) }; up()
        }
        rule.waitForIdle()
        assertEquals(2 to 1, Pantalla.items[0].let { it.x to it.y })
    }

    @Test fun sondaLienzoSolo() {
        Catalogo.cargar(ctx); Skin.cargar(ctx)
        Biblioteca.juegos.add(Juego("1", "Zelda de prueba", Tipo.ROM, sistema = "gba", archivo = "Zelda", uri = "content://x"))
        Pantalla.items.add(e("e1", 0, 0, 1, 1).copy(ref = Biblioteca.juegos[0].clave))
        rule.setContent { PorticoTheme { Box(Modifier.fillMaxSize().padding(20.dp)) { Lienzo(true, {}, {}, {}) } } }
        rule.onNodeWithTag("asa-e1").performTouchInput {
            down(center); repeat(12) { moveBy(androidx.compose.ui.geometry.Offset(celdaPx * 1.1f / 12, celdaPx * 1.1f / 12)) }; up()
        }
        rule.waitForIdle()
        assertEquals(2 to 2, Pantalla.items.first { it.id == "e1" }.let { it.w to it.h })
    }

    @Test fun elTituloCabeSiempreEnteroYCentrado() {
        val titulos = listOf("Zelda", "Pokémon Esmeralda", "The Legend of Zelda: The Minish Cap", "Supercalifragilisticoespialidoso", "Hollow Knight (SNES hack) edición coleccionista")
        val tamanos = listOf(250 to 60, 250 to 120, 300 to 200, 150 to 80)
        val fallos = ArrayList<String>()
        rule.setContent {
            val m = androidx.compose.ui.text.rememberTextMeasurer()
            val base = androidx.compose.ui.text.TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            titulos.forEach { t -> tamanos.forEach { (w, h) ->
                val sp = tamanoQueCabe(m, t, base, w, h, 44f, 6f)
                val st = base.copy(fontSize = androidx.compose.ui.unit.TextUnit(sp, androidx.compose.ui.unit.TextUnitType.Sp), lineHeight = androidx.compose.ui.unit.TextUnit(sp * 1.08f, androidx.compose.ui.unit.TextUnitType.Sp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                val r = m.measure(t, st, constraints = androidx.compose.ui.unit.Constraints(maxWidth = w))
                val palabraAncha = t.split(' ').maxOf { m.measure(it, st, softWrap = false).size.width }
                if (sp > 6.01f && (r.size.height > h || palabraAncha > w)) fallos += "$t en ${w}x$h a $sp: alto ${r.size.height}, palabra $palabraAncha"
                if (sp < 6f || sp > 44f) fallos += "$t fuera de rango $sp"
            } }
        }
        rule.waitForIdle()
        assertTrue(fallos.joinToString("\n"), fallos.isEmpty())
    }

    @Test fun elTituloCortoSeHaceGrandeYElLargoPequeno() {
        var corto = 0f; var largo = 0f
        rule.setContent {
            val m = androidx.compose.ui.text.rememberTextMeasurer(); val base = androidx.compose.ui.text.TextStyle()
            corto = tamanoQueCabe(m, "Zelda", base, 300, 100, 60f, 6f); largo = tamanoQueCabe(m, "The Legend of Zelda: The Minish Cap Collector Edition", base, 300, 100, 60f, 6f)
        }
        rule.waitForIdle(); assertTrue("$corto $largo", corto > largo * 1.5f)
    }

    @Test fun elCodigoDeColor() {
        assertEquals(0xFF4F8A5B.toInt(), hexAColor("#4F8A5B")); assertEquals(0xFF4F8A5B.toInt(), hexAColor(" 4f8a5b "))
        assertNull(hexAColor("#12345")); assertNull(hexAColor("#GGGGGG")); assertNull(hexAColor(""))
        assertEquals("#4F8A5B", colorAHex(0xFF4F8A5B.toInt()))
    }

    @Test fun losColoresUsadosQuedanParaOtrasFichas() {
        Pantalla.coloresGuardados.clear()
        Pantalla.items.addAll(listOf(e("a", 0, 0, color = 0xFF112233.toInt()), e("b", 1, 0, color = 0xFF112233.toInt()), e("c", 2, 0, color = 0xFF445566.toInt()), e("d", 3, 0)))
        Pantalla.guardarColor(ctx, 0xFFAABBCC.toInt()); Pantalla.guardarColor(ctx, 0xFF112233.toInt())
        val d = Pantalla.coloresDisponibles()
        assertEquals("guardados primero, luego los de las fichas, sin repetir ni el del tema", listOf(0xFF112233.toInt(), 0xFFAABBCC.toInt(), 0xFF445566.toInt()), d)
        Pantalla.olvidarColor(ctx, 0xFFAABBCC.toInt()); assertFalse(Pantalla.coloresDisponibles().contains(0xFFAABBCC.toInt()))
        Pantalla.coloresGuardados.clear(); Pantalla.cargarColores(ctx)
        assertTrue("se recuerdan al reabrir", Pantalla.coloresGuardados.contains(0xFF112233.toInt()))
    }

    @Test fun elEncuadreSeGuardaYViaja() {
        Catalogo.cargar(ctx); Skin.cargar(ctx)
        Pantalla.items.add(Elemento("i1", Elemento.IMAGEN, "i1.jpg", 0, 0, 2, 2, 0, 1))
        rule.setContent { PorticoTheme { Surface(Modifier.fillMaxSize(), color = Paper) { PantInicio({}, {}, {}, {}, {}) } } }
        rule.onNodeWithText("EDITAR").performClick()
        rule.onNodeWithTag("el-i1").performTouchInput { click(center) }
        rule.onNodeWithText("Encuadrar la imagen", substring = true, ignoreCase = true).performClick()
        rule.onNodeWithTag("encuadre").performTouchInput { pinch(androidx.compose.ui.geometry.Offset(center.x - 20, center.y), androidx.compose.ui.geometry.Offset(center.x - 60, center.y), androidx.compose.ui.geometry.Offset(center.x + 20, center.y), androidx.compose.ui.geometry.Offset(center.x + 60, center.y)) }
        rule.onNodeWithTag("encuadre").performTouchInput { down(center); repeat(8) { moveBy(androidx.compose.ui.geometry.Offset(-width * 0.04f, height * 0.03f)) }; up() }
        rule.onNodeWithText("GUARDAR").performClick()
        rule.waitForIdle()
        val i = Pantalla.items.first { it.id == "i1" }
        assertTrue("zoom ${i.zoom}", i.zoom > 1.2f)
        assertTrue("sigue al dedo hacia la izquierda: ${i.ox}", i.ox < -0.1f); assertTrue("y hacia abajo: ${i.oy}", i.oy > 0.1f)
        val j = elementoDeJson(elementoAJson(i.copy(ox = 0.5f, oy = -0.25f)))!!
        assertEquals(0.5f, j.ox, 0.001f); assertEquals(-0.25f, j.oy, 0.001f); assertEquals(i.zoom, j.zoom, 0.001f)
        assertEquals("sin encuadre no añade campos", 1f, elementoDeJson(elementoAJson(e("x", 0, 0)))!!.zoom, 0.0f)
    }

    @Test fun nuncaSeParteUnaPalabraAlPintar() {
        Skin.cargar(ctx)
        val titulos = listOf("PokeCatcher", "Pokémon Esmeralda", "Supercalifragilisticoespialidoso", "Zelda")
        val anchos = listOf(70, 95, 130, 180)
        rule.setContent { PorticoTheme { Column(Modifier) {
            titulos.forEachIndexed { i, t -> anchos.forEachIndexed { k, w ->
                Box(Modifier.size(w.dp, 56.dp).testTag("c-$i-$k")) { TituloAjustado(t, androidx.compose.ui.graphics.Color.Black, Modifier.fillMaxSize()) }
            } }
        } } }
        rule.waitForIdle()
        val malos = ArrayList<String>()
        titulos.forEachIndexed { i, t -> anchos.forEachIndexed { k, w ->
            val r = ArrayList<androidx.compose.ui.text.TextLayoutResult>()
            val nodo = rule.onNodeWithTag("c-$i-$k", useUnmergedTree = true).onChildren().onFirst().fetchSemanticsNode()
            nodo.config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult].action?.invoke(r)
            if (r.isEmpty()) malos += "$t en ${w}dp: sin texto" else if (partePalabras(r[0], t)) malos += "$t en ${w}dp"
        } }
        assertTrue(malos.joinToString(), malos.isEmpty())
    }

    @Test fun lasImagenesDeInternetSeGuardanSinPerderCalidad() {
        val bmp = android.graphics.Bitmap.createBitmap(3000, 4000, android.graphics.Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF336699.toInt()) }
        val bytes = java.io.ByteArrayOutputStream().also { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 97, it) }.toByteArray()
        val ruta = kotlinx.coroutines.runBlocking { guardarPortadaBytes(ctx, "q", bytes, original = true) }!!
        assertArrayEquals("mismos bytes, sin recomprimir", bytes, java.io.File(ruta).readBytes())
        val o = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }; android.graphics.BitmapFactory.decodeFile(ruta, o)
        assertEquals(3000, o.outWidth); assertEquals(4000, o.outHeight)
        // La portada normal ya no se reduce a 1000×1400
        val normal = kotlinx.coroutines.runBlocking { guardarPortadaBytes(ctx, "q2", bytes) }!!
        android.graphics.BitmapFactory.decodeFile(normal, o); assertTrue("${o.outWidth}x${o.outHeight}", o.outWidth >= 1500 && o.outHeight >= 2000)
    }
}
