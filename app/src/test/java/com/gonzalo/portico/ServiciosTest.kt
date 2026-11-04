package com.gonzalo.portico

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Las horas de Steam y las claves de Atrio. Sin red. */
class ServiciosTest {
    @Test fun lasHorasDeSteamSeLeenYSePonenPorNombreSinPisarAlPc() {
        val txt = """{"response":{"game_count":3,"games":[{"appid":1,"name":"Hades","playtime_forever":2400},{"appid":2,"name":"Celeste","playtime_forever":0},{"appid":3,"name":"Elden Ring","playtime_forever":5000}]}}"""
        val o = Steam.deJson(txt)
        assertEquals(3, o.size); assertEquals(2400, o[0].minutes)
        Biblioteca.juegos.clear(); Biblioteca.borrados.clear(); Biblioteca.cargada = true
        Biblioteca.juegos.add(nuevaFicha("Hades", "PC", anadido = 1).copy(msPropios = 2000 * 60_000L, msJugados = 2000 * 60_000L))
        Biblioteca.juegos.add(nuevaFicha("Celeste", "PC", anadido = 2))
        Biblioteca.juegos.add(Juego("pc1", "Elden Ring", Tipo.PC, pcKey = "steam:3", pcDev = "pc1", ajenosMs = mapOf("pc:pc1" to 5100 * 60_000L), msJugados = 5100 * 60_000L))
        assertEquals(1, Steam.aplicar(o))
        assertEquals("Steam sustituye lo que vino de Atrio, no se suma", 2400 * 60_000L, Biblioteca.porClave("juego:hades")!!.msJugados); assertEquals(2400 * 60_000L, Biblioteca.porClave("juego:hades")!!.ajenosMs["steam"])
        assertEquals("sin horas en Steam, nada", 0L, Biblioteca.porClave("juego:celeste")!!.msJugados)
        assertEquals("el del PC ya las trae: no se suman dos veces", 5100 * 60_000L, Biblioteca.porClave("pc:steam:3")!!.msJugados)
        assertEquals("repetir no cambia nada", 0, Steam.aplicar(o))
        assertTrue(runCatching { Steam.deJson("""{"response":{}}""") }.exceptionOrNull() is IllegalArgumentException)
        Biblioteca.juegos.clear()
    }

    @Test fun lasClavesDeAtrioSeCogenDeSusDatos() {
        val b = JSONObject().put("keys", JSONObject().put("sgdb", "abc").put("steamKey", "k1").put("steamUser", "https://steamcommunity.com/id/gonzalo/"))
        val k = Servicios.deBlobs(listOf(JSONObject(), b))
        assertEquals("abc", k.sgdb); assertEquals("k1", k.steamKey); assertEquals("https://steamcommunity.com/id/gonzalo/", k.steamUser)
    }
}
