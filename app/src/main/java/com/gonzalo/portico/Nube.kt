package com.gonzalo.portico

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Tu kDrive (API de Infomaniak), la misma que usa Atrio: por ahora para lo que hace falta aquí, que es leer la carpeta
 * «Mi carpeta/versiones» y bajar de ella las versiones nuevas de Pórtico. El token (y el número del kDrive) se guardan solo
 * en este móvil, como en Atrio: Atrio no los comparte con otras apps, así que hay que pegarlos aquí una vez.
 */
object KDrive {
    /** Se cambia en las pruebas por un kDrive simulado. */
    var api = "https://api.infomaniak.com"

    class Fail(msg: String) : IOException(msg)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("nube", Context.MODE_PRIVATE)
    fun token(ctx: Context) = prefs(ctx).getString("token", "").orEmpty()
    fun drive(ctx: Context) = prefs(ctx).getString("drive", "").orEmpty()
    fun on(ctx: Context) = token(ctx).isNotBlank() && drive(ctx).isNotBlank()

    fun save(ctx: Context, token: String, drive: String) {
        // El id del kDrive puede venir con la dirección entera («…/app/drive/123456/…»)
        val d = Regex("""drive/(\d+)""").find(drive)?.groupValues?.get(1) ?: drive.filter { it.isDigit() }
        prefs(ctx).edit().putString("token", token.trim()).putString("drive", d).remove("dirs").remove("home").remove("versDir").apply()
    }

    fun forget(ctx: Context) = prefs(ctx).edit().clear().apply()

    private fun call(ctx: Context, method: String, path: String, body: String? = null): JSONObject {
        val c = URL(api + path).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 20000; c.readTimeout = 60000
        c.setRequestProperty("Authorization", "Bearer ${token(ctx)}")
        if (body != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toByteArray()) }
        }
        return answer(c)
    }

    /** {"result":"success","data":…} o {"result":"error","error":{"description":…}} */
    private fun answer(c: HttpURLConnection): JSONObject {
        val code = c.responseCode
        val txt = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        val o = runCatching { JSONObject(txt) }.getOrNull()
        if (code !in 200..299 || o?.optString("result") == "error") {
            val why = o?.optJSONObject("error")?.let { it.optString("description").ifBlank { it.optString("code") } }
            throw Fail(when (code) {
                401 -> "kDrive no acepta el token (revísalo o crea otro)"
                403 -> "El token no tiene permiso para este kDrive"
                404 -> "No encuentro ese kDrive (revisa su número)"
                else -> "kDrive: ${why ?: "error $code"}"
            })
        }
        return o ?: JSONObject()
    }

    /** Comprueba el token y el kDrive: busca tu carpeta privada («Mi carpeta»), donde está «versiones». */
    suspend fun test(ctx: Context): String = withContext(Dispatchers.IO) {
        prefs(ctx).edit().remove("home").remove("versDir").apply()
        home(ctx)
        "Conectado a tu kDrive"
    }

    private class Item(val name: String, val id: Long, val dir: Boolean, val visibility: String)

    /** Lo que hay dentro de una carpeta. */
    private fun children(ctx: Context, dir: Long): List<Item> {
        val out = mutableListOf<Item>()
        var cursor = ""
        do {
            val q = if (cursor.isNotEmpty()) "?cursor=${URLEncoder.encode(cursor, "UTF-8")}" else ""
            val o = call(ctx, "GET", "/3/drive/${drive(ctx)}/files/$dir/files$q")
            val a = o.optJSONArray("data") ?: JSONArray()
            for (i in 0 until a.length()) a.optJSONObject(i)?.let { out += Item(it.optString("name"), it.optLong("id"), it.optString("type") == "dir", it.optString("visibility")) }
            cursor = if (o.optBoolean("has_more")) o.optString("cursor") else ""
        } while (cursor.isNotEmpty() && out.size < 5000)
        return out
    }

    /**
     * Tu carpeta privada («Mi carpeta»): en la raíz del kDrive no se puede escribir. La app oficial de kDrive la
     * reconoce por su visibilidad «is_private_space»; es la única carpeta así.
     */
    fun home(ctx: Context): Long {
        prefs(ctx).getLong("home", 0L).takeIf { it > 0 }?.let { return it }
        val root = children(ctx, 1)
        val h = root.firstOrNull { it.dir && it.visibility == "is_private_space" }
            ?: throw Fail("No encuentro tu carpeta privada en el kDrive (hay: ${root.joinToString { it.name }.ifBlank { "nada" }})")
        prefs(ctx).edit().putLong("home", h.id).apply()
        return h.id
    }

    /** Los archivos de «Mi carpeta/versiones» (nombre, id): ahí se sube cada versión de la app para que se actualice sola. */
    fun versiones(ctx: Context): List<Pair<String, Long>> {
        // El id de la carpeta se recuerda: así cada comprobación es una sola consulta (y si ya no vale, se busca de nuevo)
        prefs(ctx).getLong("versDir", 0L).takeIf { it > 0 }?.let { id ->
            runCatching { return children(ctx, id).filter { !it.dir }.map { it.name to it.id } }
            prefs(ctx).edit().remove("versDir").apply()
        }
        val dir = children(ctx, home(ctx)).firstOrNull { it.dir && it.name == "versiones" } ?: return emptyList()
        prefs(ctx).edit().putLong("versDir", dir.id).apply()
        return children(ctx, dir.id).filter { !it.dir }.map { it.name to it.id }
    }

    /**
     * Baja un archivo a [dest] (primero a un temporal: un corte no deja un archivo a medias). Los grandes se bajan en
     * cuatro trozos a la vez (kDrive acepta rangos) y es mucho más rápido; si no, de una vez.
     */
    fun download(ctx: Context, file: Long, dest: File) {
        if (runCatching { descargaEnTrozos(ctx, file, dest) }.getOrDefault(false)) return
        descargaSimple(ctx, file, dest)
    }

    private const val TROZOS = 4
    private const val MINIMO_EN_TROZOS = 4L * 1024 * 1024

    /** Devuelve false si no vale la pena (pequeño) o el servidor no entiende rangos: entonces se baja de una vez. */
    private fun descargaEnTrozos(ctx: Context, file: Long, dest: File): Boolean {
        val url = "$api/2/drive/${drive(ctx)}/files/$file/download"
        fun abre(desde: Long, hasta: Long): HttpURLConnection {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 20000; c.readTimeout = 300000
            c.setRequestProperty("Authorization", "Bearer ${token(ctx)}")
            c.setRequestProperty("Range", "bytes=$desde-$hasta")
            return c
        }
        // Primero un pedacito: dice cuánto pesa y si entiende rangos
        val sonda = abre(0, 0)
        if (sonda.responseCode != 206) { sonda.disconnect(); return false }
        val total = sonda.getHeaderField("Content-Range")?.substringAfter('/')?.toLongOrNull() ?: run { sonda.disconnect(); return false }
        sonda.inputStream.use { it.readBytes() }
        if (total < MINIMO_EN_TROZOS) return false
        dest.parentFile?.mkdirs()
        val tmp = File(dest.path + ".part")
        java.io.RandomAccessFile(tmp, "rw").use { it.setLength(total) }
        val trozo = (total + TROZOS - 1) / TROZOS
        val fallo = java.util.concurrent.atomic.AtomicReference<Throwable?>(null)
        val hilos = (0 until TROZOS).map { i ->
            Thread {
                try {
                    val a = i * trozo
                    val b = minOf(total - 1, a + trozo - 1)
                    if (a > b) return@Thread
                    var intentos = 0
                    while (true) {
                        try {
                            val c = abre(a, b)
                            if (c.responseCode != 206) throw Fail("kDrive: error ${c.responseCode}")
                            c.inputStream.use { inp ->
                                java.io.RandomAccessFile(tmp, "rw").use { f ->
                                    f.seek(a)
                                    val buf = ByteArray(64 * 1024); var pos = a
                                    while (true) { val n = inp.read(buf); if (n < 0) break; f.write(buf, 0, n); pos += n }
                                    if (pos != b + 1) throw IOException("trozo incompleto")
                                }
                            }
                            break
                        } catch (e: IOException) {
                            if (e is Fail || ++intentos >= 3) throw e
                            Thread.sleep(1000L * intentos)
                        }
                    }
                } catch (e: Throwable) { fallo.compareAndSet(null, e) }
            }.also { it.start() }
        }
        hilos.forEach { it.join() }
        fallo.get()?.let { tmp.delete(); throw it }
        if (tmp.length() != total) { tmp.delete(); throw Fail("kDrive: la descarga ha quedado incompleta") }
        if (!tmp.renameTo(dest)) { tmp.copyTo(dest, true); tmp.delete() }
        return true
    }

    private fun descargaSimple(ctx: Context, file: Long, dest: File) {
        val c = URL("$api/2/drive/${drive(ctx)}/files/$file/download").openConnection() as HttpURLConnection
        c.connectTimeout = 20000; c.readTimeout = 300000
        c.setRequestProperty("Authorization", "Bearer ${token(ctx)}")
        if (c.responseCode !in 200..299) { answer(c); throw Fail("kDrive: error ${c.responseCode}") }
        dest.parentFile?.mkdirs()
        val tmp = File(dest.path + ".part")
        c.inputStream.use { inp -> tmp.outputStream().use { inp.copyTo(it) } }
        if (!tmp.renameTo(dest)) { tmp.copyTo(dest, true); tmp.delete() }
    }

    /** ¿Responde tu kDrive con este token? (lista su raíz; falla si el número o el token no valen). */
    fun reach(ctx: Context) { children(ctx, 1) }
}
