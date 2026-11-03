package com.gonzalo.portico

import org.junit.Assert.*
import org.junit.Test

/** Las reglas de mezcla entre tus Pórticos, sin móvil ni red. */
class MezclaTest {
    private var n = 0
    private fun rom(titulo: String, arch: String = titulo, sis: String = "gba", uri: String = "content://x/$arch", mod: Long = 100, msp: Long = 0, vp: Int = 0, fav: Boolean = false) =
        Juego(id = "l${n++}", titulo = titulo, tipo = Tipo.ROM, sistema = sis, uri = uri, archivo = arch, mod = mod, msPropios = msp, msJugados = msp, vecesPropias = vp, veces = vp, favorito = fav, anadido = 50)

    private fun blobDe(dev: String, js: List<Juego>, bor: Map<String, Long> = emptyMap()) = Mezcla.deJson(Mezcla.aJson(Mezcla.aBlob(dev, js, bor)))!!
    private fun mezclar(l: List<Juego>, r: List<Blob>, b: Map<String, Long> = emptyMap(), ahora: Long = 1_000) =
        Mezcla.mezclar(l, b, r, "A", ahora) { "nuevo${n++}" }

    @Test fun laClaveNoDependeDeLaCarpeta() {
        assertEquals(rom("Zelda", "Zelda (E)", uri = "content://a").clave, rom("Zelda", "zelda (e)", uri = "content://b").clave)
    }

    @Test fun unJuegoNuevoDeOtroDispositivoSeCreaSinArchivo() {
        val r = mezclar(emptyList(), listOf(blobDe("B", listOf(rom("Zelda", msp = 5_000, vp = 2)))))
        assertTrue(r.cambio); assertEquals(1, r.juegos.size)
        val j = r.juegos[0]
        assertTrue(j.enOtroDispositivo); assertEquals(5_000L, j.msJugados); assertEquals(2, j.veces); assertEquals(0L, j.msPropios)
    }

    @Test fun lasHorasSeSumanPorDispositivo() {
        val mia = rom("Zelda", msp = 3_000, vp = 1)
        val r = mezclar(listOf(mia), listOf(blobDe("B", listOf(rom("Zelda", msp = 7_000, vp = 4)))))
        assertEquals(10_000L, r.juegos[0].msJugados); assertEquals(5, r.juegos[0].veces); assertEquals(3_000L, r.juegos[0].msPropios)
        // Repetir la mezcla con lo mismo no cambia nada ni suma dos veces
        val r2 = mezclar(r.juegos, listOf(blobDe("B", listOf(rom("Zelda", msp = 7_000, vp = 4)))))
        assertFalse(r2.cambio); assertEquals(10_000L, r2.juegos[0].msJugados)
    }

    @Test fun ganaElCambioMasReciente() {
        val l = rom("Zelda", mod = 100, fav = false)
        val r = mezclar(listOf(l), listOf(blobDe("B", listOf(rom("Zelda DX", "Zelda", mod = 200, fav = true)))))
        assertEquals("Zelda DX", r.juegos[0].titulo); assertTrue(r.juegos[0].favorito)
        val r2 = mezclar(listOf(l.copy(mod = 300)), listOf(blobDe("B", listOf(rom("Zelda DX", "Zelda", mod = 200, fav = true)))))
        assertEquals("Zelda", r2.juegos[0].titulo); assertFalse(r2.juegos[0].favorito)
    }

    @Test fun elBorradoSePropagaSalvoQueSeCambieDespues() {
        val l = rom("Zelda", mod = 100)
        val r = mezclar(listOf(l), listOf(blobDe("B", emptyList(), mapOf(l.clave to 500L))))
        assertTrue(r.juegos.isEmpty()); assertEquals(500L, r.borrados[l.clave])
        val r2 = mezclar(listOf(l.copy(mod = 800)), listOf(blobDe("B", emptyList(), mapOf(l.clave to 500L))))
        assertEquals(1, r2.juegos.size)
        // y un juego borrado no resucita desde un dispositivo que lo sigue teniendo (su cambio es anterior al borrado)
        val r3 = mezclar(emptyList(), listOf(blobDe("C", listOf(rom("Zelda", mod = 100)))), mapOf(l.clave to 500L))
        assertTrue(r3.juegos.isEmpty())
    }

    @Test fun losPropiosBlobsSeIgnoran() {
        val r = mezclar(emptyList(), listOf(blobDe("A", listOf(rom("Zelda")))))
        assertFalse(r.cambio); assertTrue(r.juegos.isEmpty())
    }

    @Test fun losBorradosMuyViejosSeOlvidan() {
        val r = mezclar(emptyList(), emptyList(), mapOf("rom:gba:x" to 1L), ahora = Mezcla.OLVIDO + 10)
        assertTrue(r.borrados.isEmpty())
    }

    @Test fun deJsonConTextoRotoNoFalla() { assertNull(Mezcla.deJson("{no es json")); assertNull(Mezcla.deJson("")) }

    @Test fun lasHorasEditadasAManoNoBajanDeLasAjenas() {
        val j = rom("Zelda", msp = 3_000).copy(ajenosMs = mapOf("B" to 7_000L), msJugados = 10_000)
        assertEquals(20_000L, j.conTotalMs(20_000).msJugados); assertEquals(13_000L, j.conTotalMs(20_000).msPropios)
        assertEquals(0L, j.conTotalMs(1_000).msPropios); assertEquals(7_000L, j.conTotalMs(1_000).msJugados)
    }
}
