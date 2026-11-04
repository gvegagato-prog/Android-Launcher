package com.gonzalo.portico

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** La suite del PC leída de la cuenta y qué juegos del PC entran en Carmesí. Sin red. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SuiteTest {
    private fun arbol(game: JSONObject? = null, online: Boolean = true, at: Long = 1_000_000L, lib: List<JSONObject> = emptyList()) = JSONObject()
        .put("live", JSONObject()
            .put("pc1", JSONObject().put("app", "atrio").put("name", "torre").put("dn", "Torre").put("os", "win32").put("on", online).put("at", at).put("today", 42)
                .put("st", JSONObject().put("on", true).put("run", false).put("name", "torre").put("lan", JSONArray().put("192.168.1.10")).put("mac", JSONArray().put("aa:bb:cc:dd:ee:ff")))
                .apply { if (game != null) put("game", game) })
            .put("yo", JSONObject().put("app", "carmesi").put("on", true).put("at", at)))
        .put("lib", JSONObject().put("pc1", JSONObject().put("name", "torre").put("games", JSONArray().also { a -> lib.forEach { a.put(it) } })))
    private fun g(k: String, n: String, m: Int = 0, lp: Long = 0, c: String = "https://x/$k.jpg") = JSONObject().put("k", k).put("n", n).put("m", m).put("lp", lp).put("c", c)
    private fun live(k: String, n: String, s: Long) = JSONObject().put("key", k).put("name", n).put("s", s).put("sg", 7L)

    @Test fun leeDispositivosBibliotecaYSteam() {
        val v = Suite.leer(arbol(game = live("steam:1", "Hades", 900_000L), lib = listOf(g("steam:1", "Hades", 1500, 950_000L), g("epic:2", "Alan Wake"))), "yo", now = 1_000_000L)
        assertEquals(1, v.devices.size)
        val pc = v.devices[0]
        assertEquals("Torre", pc.name); assertTrue(pc.online); assertEquals(42, pc.today); assertEquals("Hades", pc.game?.name); assertEquals(900_000L, pc.game?.start)
        assertEquals(listOf("aa:bb:cc:dd:ee:ff"), pc.st?.macs); assertTrue(pc.st!!.on)
        assertEquals(2, v.libs["pc1"]?.size); assertEquals(1500, v.libs["pc1"]!![0].minutes); assertEquals("torre", v.nombres["pc1"])
        // Sin latido reciente, desconectado
        assertFalse(Suite.leer(arbol(at = 1_000L), "yo", now = 1_000_000L).devices[0].online)
    }

    @Test fun laListaNegraTapaJuegos() {
        Negra.poner(listOf("Wallpaper Engine"))
        val v = Suite.leer(arbol(game = live("steam:9", "Wallpaper Engine", 1L), lib = listOf(g("steam:9", "Wallpaper Engine", 99), g("steam:1", "Hades", 1))), "yo", now = 1_000_000L)
        assertNull(v.devices[0].game); assertEquals(listOf("Hades"), v.libs["pc1"]!!.map { it.name })
        Negra.poner(emptyList())
        assertEquals(listOf("Wallpaper Engine"), Negra.deBlobs(listOf(JSONObject().put("negra", JSONArray().put(JSONObject().put("id", 1).put("nombre", "Wallpaper Engine"))))))
    }

    @Test fun entranSolosLosJugadosYLosQueYaTenias() {
        Biblioteca.juegos.clear(); Biblioteca.borrados.clear(); Biblioteca.cargada = true
        Biblioteca.juegos.add(nuevaFicha("Celeste", "PC", anadido = 1).conEstado(Estado.PASADO, 5).copy(msPropios = 500 * 60_000L, msJugados = 500 * 60_000L))
        val ahora = 10_000_000_000L
        val v = Suite.leer(arbol(lib = listOf(g("steam:1", "Hades", 1500, ahora - 3600_000L), g("steam:2", "Celeste"), g("steam:3", "Sin tocar"))), "yo", now = ahora)
        JuegosPc.aplicar(org.robolectric.RuntimeEnvironment.getApplication(), v, emptyList(), ahora)
        val hades = Biblioteca.porClave("pc:steam:1")!!
        assertEquals(Tipo.PC, hades.tipo); assertEquals(Estado.JUGANDO, hades.estado); assertEquals(1500 * 60_000L, hades.msJugados); assertEquals(1500 * 60_000L, hades.ajenosMs["pc:pc1"])
        assertEquals("https://x/steam:1.jpg", hades.portadaUrl); assertEquals(Menu.PC, menuDe(hades))
        // La ficha de Atrio pasa a ser el juego del PC, con su clave antigua de alias y su estado
        val celeste = Biblioteca.porClave("pc:steam:2")!!
        assertEquals(Tipo.PC, celeste.tipo); assertEquals(Estado.PASADO, celeste.estado); assertTrue("juego:celeste" in celeste.alias); assertEquals(celeste.id, Biblioteca.porClave("juego:celeste")?.id)
        assertEquals("las horas de Atrio eran las del PC: no se suman dos veces", 0L, celeste.msJugados)
        assertEquals(2, Biblioteca.juegos.size); assertNull(Biblioteca.porClave("pc:steam:3"))
        // Repetir no duplica ni suma dos veces
        JuegosPc.aplicar(org.robolectric.RuntimeEnvironment.getApplication(), v, emptyList(), ahora)
        assertEquals(2, Biblioteca.juegos.size); assertEquals(1500 * 60_000L, Biblioteca.porClave("pc:steam:1")!!.msJugados)
        Biblioteca.juegos.clear()
    }

    @Test fun loQueElPcEjecutaEntraYAlAcabarCuentaEnElDia() {
        Biblioteca.juegos.clear(); Biblioteca.borrados.clear(); Biblioteca.cargada = true
        val ahora = 10_000_000_000L
        val con = Suite.leer(arbol(game = live("steam:3", "Sin tocar", ahora - 30 * 60_000L), at = ahora, lib = listOf(g("steam:3", "Sin tocar"))), "yo", now = ahora)
        JuegosPc.aplicar(org.robolectric.RuntimeEnvironment.getApplication(), con, emptyList(), ahora)
        val j = Biblioteca.porClave("pc:steam:3")!!
        assertEquals(Estado.JUGANDO, j.estado)
        val sin = Suite.leer(arbol(at = ahora, lib = listOf(g("steam:3", "Sin tocar"))), "yo", now = ahora + 1000)
        JuegosPc.aplicar(org.robolectric.RuntimeEnvironment.getApplication(), sin, con.devices, ahora + 1000)
        val d = Biblioteca.porClave("pc:steam:3")!!.diasPropios
        assertEquals(1, d.size); assertTrue(d.values.first() in 29 * 60_000L..31 * 60_000L)
        Biblioteca.juegos.clear()
    }

    @Test fun laNotaDeAzabacheSeEncuentraPorClaveOAlias() {
        val blob = JSONObject().put("games", JSONArray().put(JSONObject().put("id", 5L).put("name", "Hades").put("general", 86).put("review", "Brutal").put("updated", 1L)))
        val notas = NotaAzabache.deBlobs(listOf(blob))
        assertEquals(86, notas["juego:hades"]?.general)
        val pc = Juego("a", "Hades II", Tipo.PC, pcKey = "steam:1", alias = listOf("juego:hades"))
        assertEquals(5L, NotaAzabache.de(notas, pc)?.atrioId)
        assertEquals("Brutal", NotaAzabache.de(notas, Juego("b", "Hades", Tipo.ROM))?.resena)
        assertNull(NotaAzabache.de(notas, Juego("c", "Celeste", Tipo.ROM)))
    }

    @Test fun elRecorteDelEncuadreSigueElHueco() {
        val b = android.graphics.Bitmap.createBitmap(300, 400, android.graphics.Bitmap.Config.ARGB_8888)
        assertSame("sin encuadre, la misma imagen", b, recortarEncuadre(b, 1f, 0f, 0f))
        val r = recortarEncuadre(b, 2f, 0f, 0f)
        assertEquals("ampliada al doble, se ve la mitad", 150, r.width); assertEquals(200, r.height)
    }
}
