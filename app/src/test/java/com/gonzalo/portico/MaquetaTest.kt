package com.gonzalo.portico

import org.junit.Assert.*
import org.junit.Test

class MaquetaTest {
    @Test fun elDisenoSeGuardaYSeLee() {
        val filas = listOf(
            MagRowSpec(listOf(MagCell("horas"), MagCell("texto", "Truco", "Usa la bici")), split = 0.4f, height = 120f),
            MagRowSpec(listOf(MagCell("imagen", image = "/data/x/a.jpg"))),
        )
        val vuelta = MagLayouts.deJson(MagLayouts.aJson(filas))!!
        assertEquals(filas, vuelta)
    }

    @Test fun datosRotosNoRompenNada() {
        assertNull(MagLayouts.deJson(null)); assertNull(MagLayouts.deJson("")); assertNull(MagLayouts.deJson("no es json"))
        // la línea y el alto se mantienen en su rango aunque el archivo traiga otra cosa
        val r = MagLayouts.deJson("""[{"s":0.99,"a":99999,"c":[{"t":"horas"},{"t":"veces"},{"t":"estado"}]}]""")!!.single()
        assertEquals(0.7f, r.split, 0f); assertEquals(1400f, r.height, 0f); assertEquals(2, r.cells.size)
    }

    @Test fun elDisenoDeSiempreTieneLasCasillasPedidas() {
        val tipos = MagLayouts.defaults(Tipo.ROM).flatMap { it.cells }.map { it.type }
        assertTrue(tipos.containsAll(listOf("horas", "estado", "valoracion", "apuntes")))
        assertTrue(MagLayouts.MODULOS.all { it in MAG_NOMBRES })
    }

    @Test fun elJuegoGuardaTerminadoNotaApuntesYDiseno() {
        val j = Juego("a", "Zelda", Tipo.ROM, sistema = "gba", terminado = true, finalizado = 123L, nota = 8.5f, notas = "me falta la espada", maqueta = "[]", msJugados = 7200000)
        val v = Biblioteca.aJuego(Biblioteca.aJson(j))
        assertEquals(j, v)
        // un juego guardado con la versión anterior (sin estos campos) se sigue leyendo
        val viejo = Biblioteca.aJuego(org.json.JSONObject("""{"id":"b","titulo":"X","tipo":"WEB","url":"https://x.com"}"""))
        assertFalse(viejo.terminado); assertEquals(0f, viejo.nota, 0f); assertEquals("", viejo.maqueta)
    }

    @Test fun textoDeNota() { assertEquals("—", notaTexto(0f)); assertEquals("8", notaTexto(8f)); assertEquals("8,5", notaTexto(8.5f)) }
}
