package com.gonzalo.portico

import org.junit.Assert.*
import org.junit.Test

/** Las fichas sin lanzador, los menús pequeños, las claves antiguas y la unión de dos fichas del mismo juego. Sin móvil ni red. */
class ModeloTest {
    private fun rom(titulo: String, arch: String = titulo, msp: Long = 0, vp: Int = 0) =
        Juego(id = "r-" + titulo, titulo = titulo, tipo = Tipo.ROM, sistema = "gba", uri = "content://x/$arch", archivo = arch, msPropios = msp, msJugados = msp, vecesPropias = vp, veces = vp, anadido = 50)

    @Test fun elMenuPequenoSaleDelTipoODeLaPlataforma() {
        assertEquals(Menu.EMULACION, menuDe(rom("Zelda")))
        assertEquals(Menu.ANDROID, menuDe(Juego("a", "Juego", Tipo.APP, paquete = "x.y")))
        assertEquals(Menu.WEB, menuDe(Juego("w", "PokéRogue", Tipo.WEB, url = "https://pokerogue.net")))
        assertEquals(Menu.PC, menuDePlataforma("PC")); assertEquals(Menu.PC, menuDePlataforma("Steam Deck")); assertEquals(Menu.PC, menuDePlataforma(""))
        assertEquals(Menu.EMULACION, menuDePlataforma("Switch")); assertEquals(Menu.EMULACION, menuDePlataforma("PS5")); assertEquals(Menu.EMULACION, menuDePlataforma("Xbox"))
        assertEquals(Menu.ANDROID, menuDePlataforma("Móvil")); assertEquals(Menu.WEB, menuDePlataforma("Navegador"))
    }

    @Test fun laClaveDeUnaFichaSeFijaAlNacerYNoCambiaAlRenombrar() {
        val f = nuevaFicha("Hollow Knight", "Switch", anadido = 1)
        assertEquals("juego:hollowknight", f.clave)
        assertEquals("juego:hollowknight", f.copy(titulo = "Hollow Knight: Silksong").clave)
        assertTrue(f.sinLanzador); assertFalse(f.enOtroDispositivo)
    }

    @Test fun unirSumaLasHorasYConservaLaClaveAntiguaComoAlias() {
        val ficha = nuevaFicha("Zelda", "Switch", anadido = 10).copy(estado = Estado.PASADO, terminado = true, finalizado = 500, nota = 8.5f, msPropios = 3_000, msJugados = 3_000, vecesPropias = 2, veces = 2,
            diasPropios = mapOf("2026-01-01" to 3_000L), ajenosMs = mapOf("B" to 1_000L))
        val r = rom("Zelda", msp = 1_000, vp = 1).copy(diasPropios = mapOf("2026-01-01" to 500L, "2026-01-02" to 500L))
        val u = unirJuegos(r, ficha)
        assertEquals(Tipo.ROM, u.tipo); assertEquals(r.clave, u.clave)
        assertEquals(listOf("juego:zelda"), u.alias); assertTrue("juego:zelda" in u.claves)
        assertEquals(Estado.PASADO, u.estado); assertTrue(u.terminado); assertEquals(500L, u.finalizado); assertEquals(8.5f, u.nota, 0.001f)
        assertEquals(4_000L, u.msPropios); assertEquals(5_000L, u.msJugados); assertEquals(3, u.veces)
        assertEquals(3_500L, u.diasPropios["2026-01-01"]); assertEquals(500L, u.diasPropios["2026-01-02"])
        assertEquals("Switch", u.plataforma); assertEquals(10L, u.anadido)
    }

    @Test fun laBibliotecaEncuentraUnJuegoPorSuClaveAntigua() {
        Biblioteca.juegos.clear(); Biblioteca.borrados.clear(); Biblioteca.cargada = true
        val ficha = nuevaFicha("Celeste", "PC", anadido = 1)
        Biblioteca.juegos.add(ficha)
        val app = Juego("a1", "Celeste", Tipo.APP, paquete = "com.celeste", anadido = 2)
        Biblioteca.juegos.add(app)
        assertEquals(ficha.id, Biblioteca.gemela(app)?.id)
        assertEquals(1, Biblioteca.unirGemelas())
        assertEquals(1, Biblioteca.juegos.size)
        val u = Biblioteca.juegos[0]
        assertEquals("a1", u.id); assertEquals(Tipo.APP, u.tipo); assertEquals("PC", u.plataforma)
        assertEquals(u.id, Biblioteca.porClave("juego:celeste")?.id)
        assertTrue("la ficha absorbida queda como borrada para los demás dispositivos", Biblioteca.borrados.containsKey("juego:celeste"))
        assertTrue(Biblioteca.repetido(nuevaFicha("Celeste")))
        Biblioteca.juegos.clear(); Biblioteca.borrados.clear()
    }

    @Test fun laPlataformaLosAliasYLaClaveFijaViajanConLaCuenta() {
        val f = nuevaFicha("Hades", "PC", anadido = 5).copy(alias = listOf("rom:gba:hades"), mod = 100)
        val b = Mezcla.deJson(Mezcla.aJson(Mezcla.aBlob("B", listOf(f), emptyMap())))!!
        assertEquals("juego:hades", b.juegos[0].clave); assertEquals("PC", b.juegos[0].plataforma); assertEquals(listOf("rom:gba:hades"), b.juegos[0].alias)
        val r = Mezcla.mezclar(emptyList(), emptyMap(), listOf(b), "A", 1_000) { "n1" }
        val j = r.juegos[0]
        assertEquals(Tipo.FICHA, j.tipo); assertEquals("juego:hades", j.clave); assertEquals("juego:hades", j.copy(titulo = "Hades II").clave)
        assertEquals("PC", j.plataforma); assertEquals(listOf("rom:gba:hades"), j.alias)
        // Y la ficha guardada en el móvil también
        val g = Biblioteca.aJuego(Biblioteca.aJson(j))
        assertEquals(j.clave, g.clave); assertEquals(j.alias, g.alias); assertEquals(j.plataforma, g.plataforma)
    }
}
