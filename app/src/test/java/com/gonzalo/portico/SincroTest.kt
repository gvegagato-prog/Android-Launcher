package com.gonzalo.portico

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** La cuenta y la sincronización de verdad (Firebase REST), contra el Firebase de mentira de Atrio (herramientas/fakefirebase.js). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SincroTest {
    companion object {
        private var proc: Process? = null
        private val puerto = 9000 + (System.nanoTime() % 500).toInt().let { if (it < 0) -it else it }
        private val base get() = "http://127.0.0.1:$puerto"
        private val js = listOf("../../Atrio-Suite-2.0/herramientas/fakefirebase.js", "/home/user/Atrio-Suite-2.0/herramientas/fakefirebase.js").map { File(it) }.firstOrNull { it.exists() }

        @BeforeClass @JvmStatic fun arranca() {
            org.junit.Assume.assumeTrue("falta fakefirebase.js (repo de Atrio)", js != null)
            proc = ProcessBuilder("node", js!!.path, puerto.toString()).redirectErrorStream(true).start()
            repeat(50) { runCatching { URL("$base/__dump").openStream().close(); return }; Thread.sleep(100) }
            fail("el Firebase de mentira no arranca")
        }
        @AfterClass @JvmStatic fun para() { proc?.destroy() }

        fun http(m: String, p: String, body: String? = null): String {
            val c = URL(base + p).openConnection() as HttpURLConnection
            if (m == "PATCH" || m == "PUT") { c.requestMethod = "POST"; c.setRequestProperty("X-HTTP-Method-Override", m) } else c.requestMethod = m
            if (body != null) { c.doOutput = true; c.setRequestProperty("Content-Type", "application/json"); c.outputStream.use { it.write(body.toByteArray()) } }
            return (if (c.responseCode in 200..299) c.inputStream else c.errorStream).bufferedReader().readText()
        }
    }

    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    // (los dos «móviles» comparten el proceso: lo que cada uno recuerda en su almacenamiento se guarda y se repone al cambiar)
    private val memoria = HashMap<String, Map<String, Any?>>()
    private val biblios = HashMap<String, Pair<List<Juego>, Map<String, Long>>>()
    private var actual = ""
    private fun dispositivo(id: String) {
        val sp = ctx.getSharedPreferences("portico_sync", 0)
        if (actual.isNotEmpty()) { memoria[actual] = sp.all; biblios[actual] = Biblioteca.juegos.toList() to HashMap(Biblioteca.borrados) }
        Biblioteca.juegos.clear(); Biblioteca.borrados.clear()
        biblios[id]?.let { (j, b) -> Biblioteca.juegos.addAll(j); Biblioteca.borrados.putAll(b) }
        sp.edit().clear().apply()
        memoria[id]?.forEach { (k, v) -> if (v is String) sp.edit().putString(k, v).apply() else if (v is Long) sp.edit().putLong(k, v).apply() }
        actual = id
        ctx.getSharedPreferences("portico_cuenta", 0).edit().putString("dispositivo", id).apply()
        Cuenta.cargar(ctx); SincroP.iniciar(ctx)
    }
    private fun vacia() { Biblioteca.juegos.clear(); Biblioteca.borrados.clear() }

    @Test fun dosPorticosSeSincronizan() = runBlocking {
        Cuenta.dbUrl = base; Cuenta.authBase = base
        // Atrio ya dejó su clave web y creó la cuenta
        val sign = JSONObject(http("POST", "/v1/accounts:signUp?key=K", """{"email":"gon@ronron.example.com","password":"secreto1","returnSecureToken":true}"""))
        val uid = sign.getString("localId")
        val tok = sign.getString("idToken")
        http("PUT", "/config/webKey.json?auth=$tok", "\"K\"")

        // --- Pórtico A: entra con un usuario o contraseña malos, y luego bien
        dispositivo("portico-A"); vacia(); Cuenta.salir()
        try { Cuenta.entrar("gon", "mala"); fail("debería fallar") } catch (e: Cuenta.Fail) { assertTrue(e.message!!, e.message!!.contains("incorrectos")) }
        assertEquals(uid, Cuenta.entrar("Gon ", "secreto1"))
        assertTrue(Cuenta.vinculada)

        Biblioteca.juegos.add(Juego("a1", "Zelda", Tipo.ROM, sistema = "gba", uri = "content://a", archivo = "Zelda", mod = 100, msPropios = 4_000, msJugados = 4_000, anadido = 10))
        Biblioteca.juegos.add(Juego("a2", "PokéRogue", Tipo.WEB, url = "https://pokerogue.net/", mod = 100, favorito = true, anadido = 20))
        assertTrue(SincroP.sincronizar(ctx))
        val dump = JSONObject(http("GET", "/__dump")).getJSONObject("db").getJSONObject("users").getJSONObject(uid).getJSONObject("portico")
        assertTrue(dump.getJSONObject("dev").has("portico-A")); assertTrue(dump.getJSONObject("stamps").has("portico-A"))

        // --- Pórtico B (otro móvil, biblioteca vacía): entra y recibe lo de A
        dispositivo("portico-B"); vacia(); Cuenta.salir()
        Cuenta.entrar("gon", "secreto1")
        assertTrue(SincroP.sincronizar(ctx))
        assertEquals(2, Biblioteca.juegos.size)
        val z = Biblioteca.juegos.first { it.titulo == "Zelda" }
        assertTrue(z.enOtroDispositivo); assertEquals(4_000L, z.msJugados)
        assertTrue(Biblioteca.juegos.first { it.tipo == Tipo.WEB }.favorito)

        // B añade la ROM (la encuentra el escaneo) y juega 6 s; B quita PokéRogue
        val i = Biblioteca.juegos.indexOf(z)
        Biblioteca.juegos[i] = z.copy(uri = "content://b", archivo = "Zelda", msPropios = 6_000, msJugados = 10_000)
        Biblioteca.quitar(Biblioteca.juegos.first { it.tipo == Tipo.WEB }.id)
        assertTrue(SincroP.subir(ctx, forzar = true))

        // --- Pórtico A vuelve: ve las horas sumadas y que PokéRogue se quitó
        dispositivo("portico-A")
        assertTrue(SincroP.bajar(ctx))
        assertEquals(1, Biblioteca.juegos.size)
        assertEquals(10_000L, Biblioteca.juegos[0].msJugados); assertEquals(4_000L, Biblioteca.juegos[0].msPropios)
        assertFalse(Biblioteca.juegos[0].enOtroDispositivo)
        // sin novedades: no baja nada
        assertFalse(SincroP.bajar(ctx))

        // La contraseña no se guarda
        val guardado = ctx.getSharedPreferences("portico_cuenta", 0).all.values.joinToString("|")
        assertFalse(guardado.contains("secreto1"))
        Cuenta.salir(); assertFalse(Cuenta.vinculada)
    }
}
