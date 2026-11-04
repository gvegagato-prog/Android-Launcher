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

    @Test fun elEncuadreViajaEnElJson() {
        val i = Elemento("i1", Elemento.IMAGEN, "i1.jpg", 0, 0, 2, 2, 0, 1, zoom = 1.5f)
        val j = elementoDeJson(elementoAJson(i.copy(ox = 0.5f, oy = -0.25f)))!!
        assertEquals(0.5f, j.ox, 0.001f); assertEquals(-0.25f, j.oy, 0.001f); assertEquals(i.zoom, j.zoom, 0.001f)
        assertEquals(0xFF123456.toInt(), elementoDeJson(elementoAJson(i.copy(acento = 0xFF123456.toInt())))!!.acento)
        assertEquals("sin acento elegido, no se inventa ninguno", 0, elementoDeJson(elementoAJson(i))!!.acento)
    }
}
