package com.gonzalo.portico

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** En qué punto estás con el juego. «Pasado» = terminado; «Abandonado» = lo dejaste. */
enum class Estado(val nombre: String) { JUGANDO("Jugando"), PENDIENTE("Pendiente"), PASADO("Pasado"), ABANDONADO("Abandonado") }

/**
 * De dónde sale el juego: una ROM (con su emulador), una app/juego de Android, un juego de navegador, un juego del PC (de la biblioteca que Atrio
 * del PC publica en tu cuenta: se juega en el PC o en streaming) o solo una ficha (consola…: no se abre desde el móvil).
 */
enum class Tipo { ROM, APP, WEB, FICHA, PC }

/** Los menús pequeños de las secciones: de dónde es el juego. */
enum class Menu(val nombre: String) { PC("PC"), EMULACION("Emulación"), ANDROID("Android"), WEB("Web") }

/** En qué menú pequeño va un juego: las ROMs en Emulación, las apps en Android, las webs en Web; una ficha, según su plataforma (las consolas, en Emulación). */
fun menuDe(j: Juego): Menu = when (j.tipo) {
    Tipo.ROM -> Menu.EMULACION
    Tipo.APP -> Menu.ANDROID
    Tipo.WEB -> Menu.WEB
    Tipo.PC -> Menu.PC
    Tipo.FICHA -> menuDePlataforma(j.plataforma)
}

fun menuDePlataforma(plataforma: String): Menu {
    val p = Catalogo.norm(plataforma)
    return when {
        p.isEmpty() -> Menu.PC
        listOf("pc", "steam", "epic", "gog", "windows", "linux", "mac", "ordenador").any { p.startsWith(it) || p.endsWith(it) } || p.contains("steamdeck") -> Menu.PC
        listOf("android", "movil", "ios", "iphone", "ipad", "tablet").any { p.contains(it) } -> Menu.ANDROID
        listOf("web", "navegador", "browser").any { p.contains(it) } -> Menu.WEB
        else -> Menu.EMULACION
    }
}

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
    /** ¿Lo has terminado? (y cuándo, en milisegundos; 0 = sin fecha) */
    val terminado: Boolean = false,
    val finalizado: Long = 0L,
    /** Valoración de 0 a 10 en pasos de 0,5 (0 = sin valorar). */
    val nota: Float = 0f,
    /** Tus apuntes sobre este juego. */
    val notas: String = "",
    /** El diseño de la ficha de este juego (JSON de [MagLayouts]); "" = el de su tipo o el de siempre. */
    val maqueta: String = "",
    /** Última vez que se cambió (milisegundos): al sincronizar gana el cambio más reciente. */
    val mod: Long = 0L,
    /** Las horas y las veces que ha jugado ESTE dispositivo; las de los demás, en [ajenosMs] y [ajenosVeces] (por dispositivo).
     *  [msJugados] y [veces] son el total de todos. */
    val msPropios: Long = 0L,
    val vecesPropias: Int = 0,
    val ajenosMs: Map<String, Long> = emptyMap(),
    val ajenosVeces: Map<String, Int> = emptyMap(),
    /** Jugando, pendiente, pasado o abandonado. */
    val estado: Estado = Estado.PENDIENTE,
    /** Lo jugado cada día («AAAA-MM-DD» → ms): el de ESTE dispositivo y, por dispositivo, el de los demás. Para las barras y el Diario. */
    val diasPropios: Map<String, Long> = emptyMap(),
    val ajenosDias: Map<String, Map<String, Long>> = emptyMap(),
    /** En qué se juega (PC, Switch, PS5…): lo que dijiste tú o lo que traía Atrio. Decide el menú de las fichas sin lanzador. */
    val plataforma: String = "",
    /** Las claves con las que se conoció antes a este juego (p. ej. la de la ficha de Atrio a la que luego pusiste su ROM): así Azabache sigue enlazando su valoración. */
    val alias: List<String> = emptyList(),
    /** Solo fichas: su clave, fijada al crearla (no cambia aunque la renombres). */
    val claveFija: String = "",
    /** Juegos del PC: su clave en la biblioteca del PC (la de Atrio) y el PC donde está. */
    val pcKey: String = "",
    val pcDev: String = "",
    /** Una portada de internet (la de la biblioteca del PC): se usa cuando no has puesto ninguna. */
    val portadaUrl: String = "",
    /** El encuadre de tu portada: zoom (1 = cubre el hueco) y desplazamiento como fracción del hueco (-1..1). */
    val zoom: Float = 1f, val ox: Float = 0f, val oy: Float = 0f,
    /** Lo que suele durar (minutos; 0 = no se sabe): la historia, con extras y al completo (de Atrio: HowLongToBeat/RAWG). */
    val durHistoria: Int = 0, val durExtras: Int = 0, val durCompleto: Int = 0,
) {
    /** Lo que identifica a este juego en todos tus dispositivos (el id de cada uno es distinto). */
    val clave: String get() = claveDe(this)
    /** Una ROM que viene de otro dispositivo y todavía no tienes en este. */
    val enOtroDispositivo: Boolean get() = tipo == Tipo.ROM && uri.isBlank()
    /** Una ficha sin nada que abrir en el móvil (consola…). */
    val sinLanzador: Boolean get() = tipo == Tipo.FICHA
    /** Un juego del PC: se juega en el PC o en streaming, no en el móvil. */
    val enPc: Boolean get() = tipo == Tipo.PC
    /** Todas las claves por las que se le conoce (la de ahora y las de antes). */
    val claves: List<String> get() = (listOf(clave) + alias).distinct()
    /** ¿Has movido o ampliado la portada? */
    val encuadrada: Boolean get() = zoom != 1f || ox != 0f || oy != 0f
    /** Cuánto llevas de la historia (0–1), si se sabe lo que dura. */
    val avance: Float? get() = if (durHistoria > 0 && msJugados > 0) (msJugados / 60_000f / durHistoria).coerceIn(0f, 1f) else null
}

/** Una ficha nueva sin lanzador (su clave queda fijada al nacer). */
fun nuevaFicha(titulo: String, plataforma: String = "", anadido: Long = System.currentTimeMillis()): Juego =
    Juego(Biblioteca.nuevoId(), titulo.trim(), Tipo.FICHA, plataforma = plataforma.trim(), anadido = anadido, claveFija = "juego:" + Catalogo.norm(titulo))

/**
 * Une dos fichas del mismo juego: [destino] (la que se queda, normalmente la que tiene lanzador) absorbe a [origen]. Se suman las horas, las veces y
 * los días; del estado se queda el más avanzado; la nota, la más alta; la portada y la plataforma, las que haya; y la clave de [origen] pasa a ser un alias.
 */
fun unirJuegos(destino: Juego, origen: Juego): Juego {
    fun <K> suma(a: Map<K, Long>, b: Map<K, Long>): Map<K, Long> = (a.keys + b.keys).associateWith { (a[it] ?: 0L) + (b[it] ?: 0L) }
    val peso = mapOf(Estado.PENDIENTE to 0, Estado.ABANDONADO to 1, Estado.JUGANDO to 2, Estado.PASADO to 3)
    val estado = if ((peso[origen.estado] ?: 0) > (peso[destino.estado] ?: 0)) origen.estado else destino.estado
    val ajMs = suma(destino.ajenosMs, origen.ajenosMs)
    val ajVe = (destino.ajenosVeces.keys + origen.ajenosVeces.keys).associateWith { (destino.ajenosVeces[it] ?: 0) + (origen.ajenosVeces[it] ?: 0) }
    val ajDias = (destino.ajenosDias.keys + origen.ajenosDias.keys).associateWith { suma(destino.ajenosDias[it] ?: emptyMap(), origen.ajenosDias[it] ?: emptyMap()) }
    val msp = destino.msPropios + origen.msPropios
    val vp = destino.vecesPropias + origen.vecesPropias
    return destino.copy(
        titulo = destino.titulo.ifBlank { origen.titulo }, estado = estado, terminado = estado == Estado.PASADO,
        finalizado = if (estado == Estado.PASADO) maxOf(destino.finalizado, origen.finalizado) else 0L,
        nota = maxOf(destino.nota, origen.nota), favorito = destino.favorito || origen.favorito,
        portada = destino.portada.ifBlank { origen.portada }, portadaUrl = destino.portadaUrl.ifBlank { origen.portadaUrl },
        zoom = if (destino.portada.isBlank()) origen.zoom else destino.zoom, ox = if (destino.portada.isBlank()) origen.ox else destino.ox, oy = if (destino.portada.isBlank()) origen.oy else destino.oy,
        durHistoria = if (destino.durHistoria > 0) destino.durHistoria else origen.durHistoria, durExtras = if (destino.durExtras > 0) destino.durExtras else origen.durExtras, durCompleto = if (destino.durCompleto > 0) destino.durCompleto else origen.durCompleto, plataforma = destino.plataforma.ifBlank { origen.plataforma }, maqueta = destino.maqueta.ifBlank { origen.maqueta },
        msPropios = msp, vecesPropias = vp, diasPropios = suma(destino.diasPropios, origen.diasPropios),
        ajenosMs = ajMs, ajenosVeces = ajVe, ajenosDias = ajDias, msJugados = msp + ajMs.values.sum(), veces = vp + ajVe.values.sum(),
        ultima = maxOf(destino.ultima, origen.ultima), anadido = listOf(destino.anadido, origen.anadido).filter { it > 0 }.minOrNull() ?: destino.anadido,
        alias = (destino.alias + origen.claves).filter { it != destino.clave }.distinct()
    )
}

/** Cambia el estado (y lo que va con él: «Pasado» es lo mismo que terminado, con su fecha). */
fun Juego.conEstado(e: Estado, ahora: Long = System.currentTimeMillis()): Juego =
    copy(estado = e, terminado = e == Estado.PASADO, finalizado = if (e == Estado.PASADO) (if (terminado && finalizado > 0) finalizado else ahora) else 0L)

/** Lo jugado cada día, sumando este dispositivo y los demás. */
fun Juego.dias(): Map<String, Long> {
    val out = HashMap(diasPropios)
    ajenosDias.values.forEach { d -> d.forEach { (k, v) -> out[k] = (out[k] ?: 0L) + v } }
    return out
}

/** Corrige las horas jugadas a mano: lo que falta o sobra lo pone este dispositivo (nunca por debajo de lo que sumen los demás). */
fun Juego.conTotalMs(total: Long): Juego {
    val otros = ajenosMs.values.sum()
    val propios = (total - otros).coerceAtLeast(0L)
    return copy(msPropios = propios, msJugados = propios + otros)
}

/** Lo que se edita de un juego y viaja con la cuenta (gana el cambio más reciente). */
fun compartidos(j: Juego) = listOf(j.titulo, j.favorito, j.estado, j.terminado, j.finalizado, j.nota, j.notas, j.maqueta, j.orientacion, j.plataforma, j.alias, j.pcDev, j.portadaUrl, j.zoom, j.ox, j.oy, j.durHistoria, j.durExtras, j.durCompleto)

/** La clave de un juego: ROM = sistema + nombre del archivo; app = paquete; web = dirección; ficha = «juego:» + su título normalizado (fijado al crearla). */
fun claveDe(j: Juego): String = when (j.tipo) {
    Tipo.ROM -> "rom:${j.sistema}:${Catalogo.norm(j.archivo.ifBlank { j.titulo })}"
    Tipo.APP -> "app:${j.paquete}"
    Tipo.WEB -> "web:" + j.url.lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.").trimEnd('/')
    Tipo.FICHA -> j.claveFija.ifBlank { "juego:" + Catalogo.norm(j.titulo) }
    Tipo.PC -> "pc:" + j.pcKey
}

/** Una carpeta de ROMs que diste (para volver a escanearla). */
data class Carpeta(val uri: String, val nombre: String)

/** La biblioteca: se guarda en un solo archivo JSON dentro de la app (y se puede copiar a mano desde Ajustes). */
object Biblioteca {
    val juegos = mutableStateListOf<Juego>()
    val carpetas = mutableStateListOf<Carpeta>()
    var cargada by mutableStateOf(false)
    private var archivo: File? = null
    private var appCtx: Context? = null
    /** Los juegos que quitaste (su clave y cuándo): así los demás dispositivos también los quitan al sincronizar. */
    val borrados = HashMap<String, Long>()

    fun cargar(c: Context) {
        if (cargada) return
        archivo = File(c.filesDir, "biblioteca.json"); appCtx = c.applicationContext
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
        if (reemplazar) { juegos.clear(); carpetas.clear(); borrados.clear() }
        o.optJSONObject("borrados")?.let { b -> b.keys().forEach { k -> borrados[k] = maxOf(borrados[k] ?: 0L, b.optLong(k)) } }
        val ids = juegos.map { it.id }.toSet()
        juegos.addAll(nuevos.filter { it.id !in ids })
        val us = carpetas.map { it.uri }.toSet()
        carpetas.addAll(carp.filter { it.uri !in us })
    }

    fun aTexto(): String = JSONObject().apply {
        put("version", 1)
        put("juegos", JSONArray().apply { juegos.forEach { put(aJson(it)) } })
        put("carpetas", JSONArray().apply { carpetas.forEach { put(JSONObject().put("uri", it.uri).put("nombre", it.nombre)) } })
        put("borrados", JSONObject().also { b -> borrados.forEach { (k, v) -> b.put(k, v) } })
    }.toString()

    fun guardar() {
        val f = archivo ?: return
        runCatching { val t = File(f.path + ".tmp"); t.writeText(aTexto()); t.renameTo(f) }
        archivo?.parentFile?.let { appCtx?.let { c -> Widgets.pedir(c); SincroP.cambio(c) } }
    }

    /** Guarda un juego (apuntando que se ha cambiado ahora: al sincronizar, gana el cambio más reciente). */
    fun poner(j: Juego) {
        val i = juegos.indexOfFirst { it.id == j.id }
        // Solo cambiar la ficha (nombre, favorito, terminado, nota, apuntes, diseño…) cuenta como «cambio»: jugar (las horas y las veces)
        // no, para que un dispositivo que solo juega no pise lo que otro ha editado
        val nuevo = if (i >= 0 && compartidos(juegos[i]) == compartidos(j)) j.copy(mod = juegos[i].mod) else j.copy(mod = System.currentTimeMillis())
        if (i >= 0) juegos[i] = nuevo else { juegos.add(0, nuevo); borrados.remove(nuevo.clave) }
        guardar()
    }

    /** Pone en la biblioteca lo que ha salido de mezclar con tus otros dispositivos (sin tocar la hora de cambio de nadie). */
    fun aplicarMezcla(nuevos: List<Juego>, borr: Map<String, Long>) {
        juegos.clear(); juegos.addAll(nuevos)
        borrados.clear(); borrados.putAll(borr)
        guardar()
    }

    /** Cambia un juego a partir de su versión más reciente (así dos cambios seguidos no se pisan). */
    fun cambiar(id: String, f: (Juego) -> Juego) { por(id)?.let { poner(f(it)) } }

    fun quitar(id: String) { por(id)?.let { borrados[it.clave] = System.currentTimeMillis() }; juegos.removeAll { it.id == id }; guardar() }
    fun por(id: String): Juego? = juegos.firstOrNull { it.id == id }

    /** ¿Ya está? (misma clave: la misma ROM, app o web, aunque viniera de otro dispositivo; o un juego que la tuvo antes de unirse a otro) */
    fun repetido(j: Juego): Boolean = existente(j) != null

    /** El juego que ya tienes con esa clave (o que la tuvo antes de unirse a otro), si lo hay. */
    fun existente(j: Juego): Juego? = porClave(j.clave)

    /** El juego al que corresponde una clave (la de ahora o una antigua). */
    fun porClave(k: String): Juego? = juegos.firstOrNull { it.clave == k } ?: juegos.firstOrNull { k in it.alias }

    /** Une dos juegos (ver [unirJuegos]): [origenId] desaparece dentro de [destinoId]. Devuelve el resultado o null si falta alguno. */
    fun unir(destinoId: String, origenId: String): Juego? {
        val d = por(destinoId) ?: return null; val o = por(origenId) ?: return null
        if (d.id == o.id) return d
        val u = unirJuegos(d, o)
        juegos.removeAll { it.id == o.id }; borrados[o.clave] = System.currentTimeMillis()
        poner(u)
        return u
    }

    /** La ficha sin lanzador que se llama igual que este juego (para ofrecer unirlas), si la hay. */
    fun gemela(j: Juego): Juego? = j.takeIf { !it.sinLanzador }?.let { x -> juegos.firstOrNull { it.sinLanzador && it.id != x.id && Catalogo.norm(it.titulo) == Catalogo.norm(x.titulo) && Catalogo.norm(x.titulo).isNotEmpty() } }

    /** El juego que se llama así (por su título normalizado), si lo hay. */
    fun porTitulo(titulo: String): Juego? = Catalogo.norm(titulo).takeIf { it.isNotEmpty() }?.let { n -> juegos.firstOrNull { Catalogo.norm(it.titulo) == n } }

    /** Une cada juego con lanzador a la ficha sin lanzador que se llama igual (lo que vino de Atrio con su ROM, app o web recién añadida). Devuelve cuántas. */
    fun unirGemelas(): Int {
        var n = 0
        for (j in juegos.toList()) { val g = gemela(j) ?: continue; if (por(j.id) != null && por(g.id) != null) { unir(j.id, g.id); n++ } }
        return n
    }

    fun aJson(j: Juego) = JSONObject().apply {
        put("id", j.id); put("titulo", j.titulo); put("tipo", j.tipo.name); put("sistema", j.sistema); put("uri", j.uri); put("ruta", j.ruta)
        put("archivo", j.archivo); put("paquete", j.paquete); put("url", j.url); put("portada", j.portada); put("emulador", j.emulador)
        put("favorito", j.favorito); put("terminado", j.terminado); put("fin", j.finalizado); put("nota", j.nota.toDouble()); put("notas", j.notas); put("maqueta", j.maqueta); put("anadido", j.anadido); put("ultima", j.ultima); put("veces", j.veces); put("ms", j.msJugados); put("orient", j.orientacion)
        put("mod", j.mod); put("msp", j.msPropios); put("vp", j.vecesPropias); put("est", j.estado.name)
        put("dp", JSONObject().also { o -> j.diasPropios.forEach { (k, v) -> o.put(k, v) } })
        put("ajd", JSONObject().also { o -> j.ajenosDias.forEach { (dev, m) -> o.put(dev, JSONObject().also { x -> m.forEach { (k, v) -> x.put(k, v) } }) } })
        put("ajms", JSONObject().also { o -> j.ajenosMs.forEach { (k, v) -> o.put(k, v) } }); put("ajv", JSONObject().also { o -> j.ajenosVeces.forEach { (k, v) -> o.put(k, v) } })
        put("pl", j.plataforma); put("al", JSONArray().also { a -> j.alias.forEach { a.put(it) } }); put("k0", j.claveFija)
        put("pck", j.pcKey); put("pcd", j.pcDev); put("pu", j.portadaUrl)
        put("ez", j.zoom.toDouble()); put("eox", j.ox.toDouble()); put("eoy", j.oy.toDouble()); put("dh", j.durHistoria); put("de", j.durExtras); put("dc", j.durCompleto)
    }

    fun aJuego(o: JSONObject) = Juego(
        id = o.getString("id"), titulo = o.getString("titulo"), tipo = Tipo.entries.firstOrNull { it.name == o.optString("tipo") } ?: Tipo.ROM,
        sistema = o.optString("sistema"), uri = o.optString("uri"), ruta = o.optString("ruta"), archivo = o.optString("archivo"),
        paquete = o.optString("paquete"), url = o.optString("url"), portada = o.optString("portada"), emulador = o.optString("emulador"),
        favorito = o.optBoolean("favorito"), anadido = o.optLong("anadido"), ultima = o.optLong("ultima"), veces = o.optInt("veces"),
        msJugados = o.optLong("ms"), orientacion = o.optInt("orient"),
        // (lo guardado antes de la sincronización: todo lo jugado es de este dispositivo)
        mod = o.optLong("mod"), msPropios = if (o.has("msp")) o.optLong("msp") else o.optLong("ms"), vecesPropias = if (o.has("vp")) o.optInt("vp") else o.optInt("veces"),
        ajenosMs = o.optJSONObject("ajms")?.let { a -> a.keys().asSequence().associateWith { k -> a.optLong(k) } } ?: emptyMap(),
        ajenosVeces = o.optJSONObject("ajv")?.let { a -> a.keys().asSequence().associateWith { k -> a.optInt(k) } } ?: emptyMap(),
        terminado = o.optBoolean("terminado"), finalizado = o.optLong("fin"), nota = o.optDouble("nota", 0.0).toFloat(), notas = o.optString("notas"), maqueta = o.optString("maqueta"),
        // (lo guardado antes de los estados: terminado = pasado, el resto = pendiente)
        estado = Estado.entries.firstOrNull { it.name == o.optString("est") } ?: if (o.optBoolean("terminado")) Estado.PASADO else Estado.PENDIENTE,
        diasPropios = o.optJSONObject("dp")?.let { a -> a.keys().asSequence().associateWith { k -> a.optLong(k) } } ?: emptyMap(),
        ajenosDias = o.optJSONObject("ajd")?.let { a -> a.keys().asSequence().associateWith { dev -> a.optJSONObject(dev)?.let { x -> x.keys().asSequence().associateWith { k -> x.optLong(k) } } ?: emptyMap() } } ?: emptyMap(),
        plataforma = o.optString("pl"), alias = o.optJSONArray("al")?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() } } ?: emptyList(), claveFija = o.optString("k0"),
        pcKey = o.optString("pck"), pcDev = o.optString("pcd"), portadaUrl = o.optString("pu"),
        zoom = o.optDouble("ez", 1.0).toFloat(), ox = o.optDouble("eox", 0.0).toFloat(), oy = o.optDouble("eoy", 0.0).toFloat(),
        durHistoria = o.optInt("dh"), durExtras = o.optInt("de"), durCompleto = o.optInt("dc")
    )

    fun nuevoId(): String = java.util.UUID.randomUUID().toString().take(12)
}

/** Ajustes de Carmesí que no son del tema. */
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
