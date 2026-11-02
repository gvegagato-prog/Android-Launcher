package com.gonzalo.portico

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** De dónde sale el juego: una ROM (con su emulador), una app/juego de Android o un juego de navegador. */
enum class Tipo { ROM, APP, WEB }

/**
 * Un juego de la biblioteca.
 * ROM: [uri] es el documento (content://…) que diste con el selector de archivos; [ruta] la ruta real si se puede deducir.
 * [archivo] es el nombre del archivo sin extensión, tal cual (sirve para buscar la portada).
 */
data class Juego(
    val id: String,
    val titulo: String,
    val tipo: Tipo,
    val sistema: String = "",
    val uri: String = "",
    val ruta: String = "",
    val archivo: String = "",
    val paquete: String = "",
    val url: String = "",
    /** Portada elegida por ti: archivo en la carpeta de la app. */
    val portada: String = "",
    /** Emulador elegido solo para este juego (la etiqueta del comando); "" = el del sistema. */
    val emulador: String = "",
    val favorito: Boolean = false,
    val anadido: Long = 0L,
    val ultima: Long = 0L,
    val veces: Int = 0,
    val msJugados: Long = 0L,
    /** Juegos web: 0 automática, 1 horizontal, 2 vertical. */
    val orientacion: Int = 0,
)

/** Una carpeta de ROMs que diste (para volver a escanearla). */
data class Carpeta(val uri: String, val nombre: String)

/** La biblioteca: se guarda en un solo archivo JSON dentro de la app (y se puede copiar a mano desde Ajustes). */
object Biblioteca {
    val juegos = mutableStateListOf<Juego>()
    val carpetas = mutableStateListOf<Carpeta>()
    var cargada by mutableStateOf(false)
    private var archivo: File? = null

    fun cargar(c: Context) {
        if (cargada) return
        archivo = File(c.filesDir, "biblioteca.json")
        val f = archivo!!
        if (f.exists()) runCatching { leer(f.readText()) }
        cargada = true
    }

    fun leer(txt: String, reemplazar: Boolean = true) {
        val o = JSONObject(txt)
        val js = o.optJSONArray("juegos") ?: JSONArray()
        val nuevos = (0 until js.length()).map { aJuego(js.getJSONObject(it)) }
        val cs = o.optJSONArray("carpetas") ?: JSONArray()
        val carp = (0 until cs.length()).map { cs.getJSONObject(it).let { x -> Carpeta(x.getString("uri"), x.optString("nombre")) } }
        if (reemplazar) { juegos.clear(); carpetas.clear() }
        val ids = juegos.map { it.id }.toSet()
        juegos.addAll(nuevos.filter { it.id !in ids })
        val us = carpetas.map { it.uri }.toSet()
        carpetas.addAll(carp.filter { it.uri !in us })
    }

    fun aTexto(): String = JSONObject().apply {
        put("version", 1)
        put("juegos", JSONArray().apply { juegos.forEach { put(aJson(it)) } })
        put("carpetas", JSONArray().apply { carpetas.forEach { put(JSONObject().put("uri", it.uri).put("nombre", it.nombre)) } })
    }.toString()

    fun guardar() {
        val f = archivo ?: return
        runCatching { val t = File(f.path + ".tmp"); t.writeText(aTexto()); t.renameTo(f) }
    }

    fun poner(j: Juego) {
        val i = juegos.indexOfFirst { it.id == j.id }
        if (i >= 0) juegos[i] = j else juegos.add(0, j)
        guardar()
    }

    fun quitar(id: String) { juegos.removeAll { it.id == id }; guardar() }
    fun por(id: String): Juego? = juegos.firstOrNull { it.id == id }

    /** ¿Ya está? (misma ROM, misma app o misma web) */
    fun repetido(j: Juego): Boolean = juegos.any {
        it.tipo == j.tipo && when (j.tipo) { Tipo.ROM -> it.uri == j.uri; Tipo.APP -> it.paquete == j.paquete; Tipo.WEB -> it.url == j.url }
    }

    fun aJson(j: Juego) = JSONObject().apply {
        put("id", j.id); put("titulo", j.titulo); put("tipo", j.tipo.name); put("sistema", j.sistema); put("uri", j.uri); put("ruta", j.ruta)
        put("archivo", j.archivo); put("paquete", j.paquete); put("url", j.url); put("portada", j.portada); put("emulador", j.emulador)
        put("favorito", j.favorito); put("anadido", j.anadido); put("ultima", j.ultima); put("veces", j.veces); put("ms", j.msJugados); put("orient", j.orientacion)
    }

    fun aJuego(o: JSONObject) = Juego(
        id = o.getString("id"), titulo = o.getString("titulo"), tipo = Tipo.entries.firstOrNull { it.name == o.optString("tipo") } ?: Tipo.ROM,
        sistema = o.optString("sistema"), uri = o.optString("uri"), ruta = o.optString("ruta"), archivo = o.optString("archivo"),
        paquete = o.optString("paquete"), url = o.optString("url"), portada = o.optString("portada"), emulador = o.optString("emulador"),
        favorito = o.optBoolean("favorito"), anadido = o.optLong("anadido"), ultima = o.optLong("ultima"), veces = o.optInt("veces"),
        msJugados = o.optLong("ms"), orientacion = o.optInt("orient")
    )

    fun nuevoId(): String = java.util.UUID.randomUUID().toString().take(12)
}

/** Ajustes de Pórtico que no son del tema. */
object Ajustes {
    /** Emulador elegido por sistema: la etiqueta del comando; sin entrada = automático. */
    val emuladorPorSistema = HashMap<String, String>()
    var portadasLibretro by mutableStateOf(true)
    /** Columnas de la biblioteca: 0 normal (3), 1 grandes (2), 2 pequeñas (4). */
    var tamano by mutableStateOf(0)
    private fun p(c: Context) = c.getSharedPreferences("portico_ajustes", Context.MODE_PRIVATE)

    fun cargar(c: Context) {
        val p = p(c)
        portadasLibretro = p.getBoolean("libretro", true)
        tamano = p.getInt("tamano", 0)
        emuladorPorSistema.clear()
        runCatching { JSONObject(p.getString("emus", "{}")!!).let { o -> o.keys().forEach { k -> emuladorPorSistema[k] = o.getString(k) } } }
    }

    fun guardar(c: Context) {
        p(c).edit().putBoolean("libretro", portadasLibretro).putInt("tamano", tamano)
            .putString("emus", JSONObject(emuladorPorSistema as Map<*, *>).toString()).apply()
    }
}
