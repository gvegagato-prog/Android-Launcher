package com.gonzalo.portico

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * La pantalla de inicio de Pórtico: un lienzo de [COLUMNAS] columnas de casillas cuadradas (invisibles) donde colocas lo que quieras,
 * con el tamaño que quieras: juegos (se enlazan por su clave, así que valen en todos tus móviles) e imágenes tuyas.
 * Cada elemento tiene su color de fondo (0 = el del tema). Se sincroniza con la cuenta: gana el cambio más reciente de cada elemento
 * y lo que quitas viaja como «borrado», como en la biblioteca.
 */
data class Elemento(
    val id: String, val tipo: String, val ref: String,
    val x: Int, val y: Int, val w: Int, val h: Int,
    /** ARGB; 0 = el del tema. */
    val color: Int = 0,
    /** Última vez que se cambió (milisegundos). */
    val mod: Long = 0L
) {
    val esJuego get() = tipo == JUEGO
    val esImagen get() = tipo == IMAGEN
    fun cabe(o: Elemento) = x < o.x + o.w && o.x < x + w && y < o.y + o.h && o.y < y + h

    companion object { const val JUEGO = "juego"; const val IMAGEN = "imagen" }
}

fun elementoAJson(e: Elemento) = JSONObject().put("id", e.id).put("t", e.tipo).put("r", e.ref).put("x", e.x).put("y", e.y).put("w", e.w).put("h", e.h).put("c", e.color).put("m", e.mod)
fun elementoDeJson(o: JSONObject): Elemento? {
    val id = o.optString("id").ifBlank { return null }
    val t = o.optString("t").takeIf { it == Elemento.JUEGO || it == Elemento.IMAGEN } ?: return null
    return Elemento(id, t, o.optString("r"), o.optInt("x"), o.optInt("y"), o.optInt("w", 1).coerceIn(1, Pantalla.COLUMNAS), o.optInt("h", 1).coerceIn(1, 12), o.optInt("c"), o.optLong("m"))
}

object Pantalla {
    const val COLUMNAS = 4
    val items = mutableStateListOf<Elemento>()
    val borrados = HashMap<String, Long>()
    private var archivo: File? = null
    private var appCtx: Context? = null
    private var cargada = false

    fun cargar(c: Context) {
        if (cargada) return
        cargada = true; appCtx = c.applicationContext
        val f = File(c.filesDir, "pantalla.json").also { archivo = it }
        if (f.exists()) runCatching {
            val o = JSONObject(f.readText())
            items.addAll(o.optJSONArray("items")?.let { a -> (0 until a.length()).mapNotNull { elementoDeJson(a.getJSONObject(it)) } } ?: emptyList())
            o.optJSONObject("borrados")?.let { b -> b.keys().forEach { borrados[it] = b.optLong(it) } }
        }
    }

    fun guardar() {
        val f = archivo ?: return
        runCatching { val t = File(f.path + ".tmp"); t.writeText(aTexto()); t.renameTo(f) }
        appCtx?.let { SincroP.cambio(it) }
    }

    fun aTexto() = JSONObject().put("items", JSONArray().also { a -> items.forEach { a.put(elementoAJson(it)) } })
        .put("borrados", JSONObject().also { b -> borrados.forEach { (k, v) -> b.put(k, v) } }).toString()

    fun nuevoId() = java.util.UUID.randomUUID().toString().replace("-", "").take(10)

    /** Un hueco libre para algo de [w]×[h] casillas (de arriba abajo, de izquierda a derecha). */
    fun hueco(w: Int, h: Int, ocupados: List<Elemento> = items.toList()): Pair<Int, Int> {
        val ww = w.coerceAtMost(COLUMNAS)
        var y = 0
        while (true) {
            for (x in 0..COLUMNAS - ww) if (ocupados.none { Elemento("", "", "", x, y, ww, h).cabe(it) }) return x to y
            y++
        }
    }

    fun poner(e: Elemento) {
        val i = items.indexOfFirst { it.id == e.id }
        val n = e.copy(mod = System.currentTimeMillis())
        if (i >= 0) items[i] = n else { items.add(n); borrados.remove(e.id) }
        guardar()
    }

    fun quitar(id: String) {
        if (items.removeAll { it.id == id }) { borrados[id] = System.currentTimeMillis(); guardar() }
    }

    /** ¿Se puede poner [e] sin pisar a otro? (y dentro de las columnas) */
    fun libre(e: Elemento) = e.x >= 0 && e.y >= 0 && e.x + e.w <= COLUMNAS && e.w >= 1 && e.h >= 1 && items.none { it.id != e.id && e.cabe(it) }

    fun filas(): Int = (items.maxOfOrNull { it.y + it.h } ?: 0)

    /** La primera vez: los últimos juegos, en baldosas de 2×1, para no empezar con la pantalla vacía. */
    fun inicial(c: Context) {
        val p = c.getSharedPreferences("portico_pantalla", Context.MODE_PRIVATE)
        if (p.getBoolean("inicial", false)) return
        p.edit().putBoolean("inicial", true).apply()
        if (items.isNotEmpty() || borrados.isNotEmpty() || Biblioteca.juegos.isEmpty() || Cuenta.vinculada) return
        recientes(Biblioteca.juegos.toList()).take(8).forEach { j ->
            val (x, y) = hueco(2, 1); items.add(Elemento(nuevoId(), Elemento.JUEGO, j.clave, x, y, 2, 1, 0, System.currentTimeMillis()))
        }
        guardar()
    }

    fun aplicarMezcla(nuevos: List<Elemento>, borr: Map<String, Long>) {
        items.clear(); items.addAll(nuevos); borrados.clear(); borrados.putAll(borr); guardar()
    }

    /** Dónde se guardan las imágenes de la pantalla. */
    fun dirImagenes(c: Context) = File(c.filesDir, "pantalla").apply { mkdirs() }
}

/** La mezcla de la pantalla de tus móviles: por elemento, gana el cambio más reciente; lo quitado se quita en todos (salvo que se cambiara después). */
object MezclaPantalla {
    class Res(val items: List<Elemento>, val borrados: Map<String, Long>, val cambio: Boolean)

    fun mezclar(locales: List<Elemento>, borrados: Map<String, Long>, remotos: List<Pair<List<Elemento>, Map<String, Long>>>, ahora: Long = System.currentTimeMillis()): Res {
        val bor = HashMap(borrados)
        remotos.forEach { (_, b) -> b.forEach { (k, t) -> if (t > (bor[k] ?: 0L)) bor[k] = t } }
        bor.entries.removeAll { ahora - it.value > Mezcla.OLVIDO }
        val mejor = LinkedHashMap<String, Elemento>()
        locales.forEach { mejor[it.id] = it }
        remotos.forEach { (l, _) -> l.forEach { e -> val a = mejor[e.id]; if (a == null || e.mod > a.mod) mejor[e.id] = e } }
        val salida = mejor.values.filter { (bor[it.id] ?: 0L) <= it.mod }
        return Res(salida, bor, salida != locales || bor != borrados)
    }
}
