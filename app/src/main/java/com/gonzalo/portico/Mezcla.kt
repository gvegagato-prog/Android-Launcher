package com.gonzalo.portico

import org.json.JSONArray
import org.json.JSONObject

/*
 * Mezclar la biblioteca de tus dispositivos. Cada dispositivo sube a tu cuenta SU biblioteca (un Blob) y baja la de los demás:
 *  - los juegos se emparejan por su clave (la misma ROM —sistema y nombre de archivo—, la misma app o la misma web);
 *  - lo que se edita de la ficha (nombre, favorito, terminado, nota, apuntes, diseño…) lo decide el cambio más reciente;
 *  - las horas y las veces se suman: cada dispositivo sube las SUYAS y el total es la suma de todos;
 *  - lo que quitas viaja como «borrado» (con su hora) y se quita en los demás, salvo que se haya vuelto a cambiar después;
 *  - un juego de otro dispositivo se crea aquí; si es una ROM, queda «en otro dispositivo» (sin archivo) hasta que la añadas.
 */

/** Lo que un dispositivo sube de cada juego: lo compartido y SUS horas y veces. */
class JuegoSync(
    val clave: String, val titulo: String, val tipo: Tipo, val sistema: String, val paquete: String, val url: String, val archivo: String,
    val favorito: Boolean, val terminado: Boolean, val finalizado: Long, val nota: Float, val notas: String, val maqueta: String, val orientacion: Int,
    val anadido: Long, val mod: Long, val msp: Long, val vp: Int, val ultima: Long,
    val estado: Estado = Estado.PENDIENTE, val dias: Map<String, Long> = emptyMap(), val plataforma: String = "", val alias: List<String> = emptyList(),
    val pcKey: String = "", val pcDev: String = "", val portadaUrl: String = ""
)

/** La biblioteca de un dispositivo, tal como la sube a la cuenta. */
class Blob(val dev: String, val juegos: List<JuegoSync>, val borrados: Map<String, Long>, val pantalla: List<Elemento> = emptyList(), val pBorrados: Map<String, Long> = emptyMap(), val ajustes: String = "",
           val sagas: List<Saga> = emptyList(), val sagasBorradas: Map<String, Long> = emptyMap())

class ResMezcla(val juegos: List<Juego>, val borrados: Map<String, Long>, val cambio: Boolean)

object Mezcla {
    /** Los borrados se olvidan a los 180 días (un dispositivo que lleve más sin conectar ya no los verá). */
    const val OLVIDO = 180L * 86_400_000L

    fun aBlob(dev: String, juegos: List<Juego>, borrados: Map<String, Long>, pantalla: List<Elemento> = emptyList(), pBorrados: Map<String, Long> = emptyMap(), ajustes: String = "",
              sagas: List<Saga> = emptyList(), sagasBorradas: Map<String, Long> = emptyMap()): Blob = Blob(dev,
        juegos.filter { it.titulo.isNotBlank() }.distinctBy { it.clave }.map { j ->
            JuegoSync(j.clave, j.titulo, j.tipo, j.sistema, j.paquete, j.url, j.archivo, j.favorito, j.terminado, j.finalizado, j.nota, j.notas, j.maqueta,
                j.orientacion, j.anadido, j.mod, j.msPropios, j.vecesPropias, j.ultima, j.estado, j.diasPropios.entries.sortedByDescending { it.key }.take(400).associate { it.key to it.value }, j.plataforma, j.alias, j.pcKey, j.pcDev, j.portadaUrl)
        }, borrados, pantalla, pBorrados, ajustes, sagas, sagasBorradas)

    fun aJson(b: Blob): String = JSONObject().put("v", 1).put("dev", b.dev)
        .put("borrados", JSONObject().also { o -> b.borrados.forEach { (k, v) -> o.put(k, v) } })
        .put("juegos", JSONArray().also { a ->
            b.juegos.forEach { g ->
                a.put(JSONObject().put("k", g.clave).put("t", g.titulo).put("tp", g.tipo.name).put("s", g.sistema).put("pk", g.paquete).put("u", g.url).put("a", g.archivo)
                    .put("f", g.favorito).put("te", g.terminado).put("fi", g.finalizado).put("n", g.nota.toDouble()).put("no", g.notas).put("m", g.maqueta)
                    .put("o", g.orientacion).put("an", g.anadido).put("mod", g.mod).put("msp", g.msp).put("vp", g.vp).put("ul", g.ultima).put("es", g.estado.name)
                    .put("dd", JSONObject().also { d -> g.dias.forEach { (k, v) -> d.put(k, v) } }).put("pl", g.plataforma).put("al", JSONArray().also { x -> g.alias.forEach { x.put(it) } }).put("pck", g.pcKey).put("pcd", g.pcDev).put("pu", g.portadaUrl))
            }
        })
        .put("pe", JSONArray().also { a -> b.pantalla.forEach { a.put(elementoAJson(it)) } })
        .put("pb", JSONObject().also { o -> b.pBorrados.forEach { (k, v) -> o.put(k, v) } })
        .apply {
            if (b.ajustes.isNotBlank()) put("aj", JSONObject(b.ajustes))
            if (b.sagas.isNotEmpty()) put("sg", JSONArray().also { a -> b.sagas.forEach { a.put(it.aJson()) } })
            if (b.sagasBorradas.isNotEmpty()) put("sgb", JSONObject().also { x -> b.sagasBorradas.forEach { (k, v) -> x.put(k, v) } })
        }.toString()

    fun deJson(s: String?): Blob? = try {
        if (s.isNullOrBlank()) null else {
            val o = JSONObject(s)
            val ja = o.optJSONArray("juegos") ?: JSONArray()
            val bo = o.optJSONObject("borrados")
            Blob(o.optString("dev"), (0 until ja.length()).mapNotNull { i ->
                val g = ja.getJSONObject(i)
                val tipo = Tipo.entries.firstOrNull { it.name == g.optString("tp") } ?: return@mapNotNull null
                JuegoSync(g.getString("k"), g.optString("t"), tipo, g.optString("s"), g.optString("pk"), g.optString("u"), g.optString("a"),
                    g.optBoolean("f"), g.optBoolean("te"), g.optLong("fi"), g.optDouble("n", 0.0).toFloat(), g.optString("no"), g.optString("m"), g.optInt("o"),
                    g.optLong("an"), g.optLong("mod"), g.optLong("msp"), g.optInt("vp"), g.optLong("ul"),
                    Estado.entries.firstOrNull { it.name == g.optString("es") } ?: if (g.optBoolean("te")) Estado.PASADO else Estado.PENDIENTE,
                    g.optJSONObject("dd")?.let { d -> d.keys().asSequence().associateWith { k -> d.optLong(k) } } ?: emptyMap(),
                    g.optString("pl"), g.optJSONArray("al")?.let { x -> (0 until x.length()).map { x.optString(it) }.filter { it.isNotBlank() } } ?: emptyList(),
                    g.optString("pck"), g.optString("pcd"), g.optString("pu"))
            }, bo?.keys()?.asSequence()?.associateWith { bo.optLong(it) } ?: emptyMap(),
                o.optJSONArray("pe")?.let { a -> (0 until a.length()).mapNotNull { elementoDeJson(a.getJSONObject(it)) } } ?: emptyList(),
                o.optJSONObject("pb")?.let { pb -> pb.keys().asSequence().associateWith { pb.optLong(it) } } ?: emptyMap(),
                o.optJSONObject("aj")?.toString().orEmpty(),
                o.optJSONArray("sg")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it)?.let(Saga::deJson) } } ?: emptyList(),
                o.optJSONObject("sgb")?.let { x -> x.keys().asSequence().associateWith { x.optLong(it) } } ?: emptyMap())
        }
    } catch (e: Exception) { null }

    /**
     * Mezcla [locales] con los [remotos] (los de los OTROS dispositivos que han cambiado). [yo] es este dispositivo (sus propios blobs se ignoran).
     * Devuelve la biblioteca nueva, los borrados y si ha cambiado algo.
     */
    fun mezclar(
        locales: List<Juego>, borrados: Map<String, Long>, remotos: List<Blob>, yo: String, ahora: Long = System.currentTimeMillis(),
        nuevoId: () -> String = { Biblioteca.nuevoId() }
    ): ResMezcla {
        val otros = remotos.filter { it.dev != yo }
        val bor = HashMap(borrados)
        otros.forEach { r -> r.borrados.forEach { (k, t) -> if (t > (bor[k] ?: 0L)) bor[k] = t } }
        bor.entries.removeAll { ahora - it.value > OLVIDO }

        // Lo de los demás, por clave (sin lo que se borró después de su último cambio)
        val remotoDe = LinkedHashMap<String, MutableList<Pair<String, JuegoSync>>>()
        otros.forEach { r -> r.juegos.forEach { g -> if ((bor[g.clave] ?: 0L) <= g.mod) remotoDe.getOrPut(g.clave) { ArrayList() }.add(r.dev to g) } }

        val vistas = HashSet<String>()
        val salida = ArrayList<Juego>()
        for (l in locales) {
            if (!vistas.add(l.clave)) continue                       // (dos iguales: se queda uno)
            if ((bor[l.clave] ?: 0L) > l.mod) continue               // quitado en otro dispositivo después de tu último cambio
            val rs = remotoDe[l.clave]
            salida += if (rs == null) l else unir(l, rs)
        }
        for ((k, rs) in remotoDe) {
            if (k in vistas) continue
            vistas += k
            salida += crear(rs, nuevoId())
        }
        // (los que ya estaban, en su sitio; los nuevos, al principio, los más recientes primero)
        val ids = locales.map { it.id }.toSet()
        val finales = salida.filter { it.id !in ids }.sortedByDescending { it.anadido } + salida.filter { it.id in ids }
        val cambio = finales != locales || bor != borrados
        return ResMezcla(finales, bor, cambio)
    }

    class ResSagas(val sagas: List<Saga>, val borradas: Map<String, Long>, val cambio: Boolean)

    /** Mezcla las sagas de este dispositivo con las de los demás: de cada una gana la que se cambió más tarde; lo borrado se borra salvo que se haya vuelto a cambiar después. */
    fun mezclarSagas(locales: List<Saga>, borradas: Map<String, Long>, remotos: List<Blob>, yo: String, ahora: Long = System.currentTimeMillis()): ResSagas {
        val otros = remotos.filter { it.dev != yo }
        val bor = HashMap(borradas)
        otros.forEach { r -> r.sagasBorradas.forEach { (k, t) -> if (t > (bor[k] ?: 0L)) bor[k] = t } }
        bor.entries.removeAll { ahora - it.value > OLVIDO }
        val por = LinkedHashMap<String, Saga>()
        for (s in locales + otros.flatMap { it.sagas }) { val cur = por[s.id]; if (cur == null || s.mod > cur.mod) por[s.id] = s }
        val vivas = por.values.filter { (bor[it.id] ?: 0L) <= it.mod }
        // (en el orden en que estaban aquí; las nuevas, al final)
        val orden = locales.map { it.id }
        val finales = vivas.sortedBy { s -> orden.indexOf(s.id).let { if (it < 0) Int.MAX_VALUE else it } }
        return ResSagas(finales, bor, finales != locales || bor != borradas)
    }

    private fun total(ms: Map<String, Long>) = ms.values.sum()

    private fun unir(l: Juego, rs: List<Pair<String, JuegoSync>>): Juego {
        val g = rs.maxByOrNull { it.second.mod }!!.second
        val base = if (g.mod > l.mod) l.copy(titulo = g.titulo.ifBlank { l.titulo }, favorito = g.favorito, estado = g.estado, terminado = g.terminado, finalizado = g.finalizado, nota = g.nota, notas = g.notas,
            maqueta = g.maqueta, orientacion = g.orientacion, mod = g.mod, plataforma = g.plataforma, alias = g.alias, pcDev = g.pcDev, portadaUrl = g.portadaUrl) else l
        val ms = l.ajenosMs + rs.associate { it.first to it.second.msp }
        val ve = l.ajenosVeces + rs.associate { it.first to it.second.vp }
        val dd = l.ajenosDias + rs.associate { it.first to it.second.dias }
        val anadidos = (listOf(l.anadido) + rs.map { it.second.anadido }).filter { it > 0 }
        return base.copy(
            ajenosMs = ms, ajenosVeces = ve, ajenosDias = dd, msJugados = base.msPropios + total(ms), veces = base.vecesPropias + ve.values.sum(),
            ultima = maxOf(l.ultima, rs.maxOf { it.second.ultima }), anadido = anadidos.minOrNull() ?: l.anadido
        )
    }

    private fun crear(rs: List<Pair<String, JuegoSync>>, id: String): Juego {
        val g = rs.maxByOrNull { it.second.mod }!!.second
        val ms = rs.associate { it.first to it.second.msp }
        val ve = rs.associate { it.first to it.second.vp }
        return Juego(
            id = id, titulo = g.titulo, tipo = g.tipo, sistema = g.sistema, uri = "", ruta = "", archivo = g.archivo, paquete = g.paquete, url = g.url,
            favorito = g.favorito, anadido = rs.map { it.second.anadido }.filter { it > 0 }.minOrNull() ?: g.anadido, ultima = rs.maxOf { it.second.ultima },
            veces = ve.values.sum(), msJugados = total(ms), orientacion = g.orientacion, terminado = g.terminado, finalizado = g.finalizado, nota = g.nota,
            notas = g.notas, maqueta = g.maqueta, mod = g.mod, msPropios = 0L, vecesPropias = 0, ajenosMs = ms, ajenosVeces = ve,
            estado = g.estado, ajenosDias = rs.associate { it.first to it.second.dias }, plataforma = g.plataforma, alias = g.alias,
            claveFija = if (g.tipo == Tipo.FICHA) g.clave else "", pcKey = g.pcKey, pcDev = g.pcDev, portadaUrl = g.portadaUrl
        )
    }
}
