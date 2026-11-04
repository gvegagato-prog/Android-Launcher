package com.gonzalo.portico

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.atomic.AtomicBoolean

/**
 * La sincronización de Carmesí con tu cuenta de Atrio, como la de Atrio entre sus dispositivos:
 *  - lo que cambias aquí (un juego, tus horas, una nota…) se sube en cuanto pasan [ESPERA] ms (por si llegan varios cambios seguidos);
 *  - mientras Carmesí se ve, se escucha tu cuenta en directo: lo que sube otro de tus Carmesís se baja y se mezcla al momento;
 *  - una sola subida o mezcla a la vez; sin conexión se reintenta solo.
 * Lo que se mezcla y cómo, en [Mezcla].
 */
object SincroP {
    const val ESPERA = 600L
    private val candado = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var app: Context? = null
    private var programada: Job? = null
    private var escucha: FbStream? = null
    private val bajando = AtomicBoolean(false)
    @Volatile private var fallos = 0

    /** Qué está pasando (para Ajustes): "" sin vincular, «Sincronizado», o el último problema. */
    var estado by mutableStateOf("")
        private set
    /** Cuándo se sincronizó por última vez (milisegundos; 0 = todavía no). */
    var ultima by mutableStateOf(0L)
        private set

    private fun prefs(c: Context) = c.getSharedPreferences("portico_sync", Context.MODE_PRIVATE)

    fun iniciar(c: Context) {
        app = c.applicationContext
        ultima = prefs(c).getLong("ultima", 0L)
    }

    // ---- lo ya visto de cada dispositivo (su hora de subida) y lo último que subí
    private fun vistos(c: Context): JSONObject = runCatching { JSONObject(prefs(c).getString("vistos", "{}")!!) }.getOrDefault(JSONObject())
    private fun guardaVistos(c: Context, o: JSONObject) = prefs(c).edit().putString("vistos", o.toString()).apply()
    fun olvidarVistos() { app?.let { prefs(it).edit().remove("vistos").remove("hash").apply() } }

    /** Algo ha cambiado aquí (Biblioteca.guardar lo avisa): se sube dentro de [ms]. */
    fun cambio(c: Context, ms: Long = ESPERA) {
        val a = app ?: run { iniciar(c); app }
        if (a == null || !Cuenta.vinculada) return
        synchronized(this) {
            programada?.cancel()
            programada = scope.launch {
                delay(ms)
                // Una subida empezada se termina siempre (si se cortara a medias, los demás no se enterarían)
                withContext(NonCancellable) { subir(a) }
            }
        }
    }

    /** Sube la biblioteca de este dispositivo si ha cambiado desde la última vez. Devuelve false si no ha podido (se reintenta solo). */
    suspend fun subir(c: Context, forzar: Boolean = false): Boolean = candado.withLock {
        if (!Cuenta.vinculada) return@withLock true
        try {
            val blob = Mezcla.aJson(Mezcla.aBlob(Cuenta.dispositivo, Biblioteca.juegos.toList(), HashMap(Biblioteca.borrados), Pantalla.items.toList(), HashMap(Pantalla.borrados), if (Skin.ajustesAt > 0L) Skin.ajustesJson().toString() else "", Sagas.lista.toList(), HashMap(Sagas.borradas), if (Inicio.config.mod > 0L) Inicio.config.aJson().toString() else ""))
            val hash = blob.hashCode().toString() + ":" + blob.length
            val p = prefs(c)
            if (!forzar && p.getString("hash", "") == hash) return@withLock true
            val nodo = JSONObject().put("name", Cuenta.nombreDispositivo).put("device", "portico").put("updated", JSONObject().put(".sv", "timestamp")).put("data", blob)
            Cuenta.db("PUT", "${Cuenta.raiz}/dev/${Cuenta.dispositivo}", nodo.toString(), "print=silent")
            val ts = Cuenta.db("GET", "${Cuenta.raiz}/dev/${Cuenta.dispositivo}/updated", null)?.toLongOrNull() ?: throw Cuenta.Fail("Firebase no ha guardado la copia.")
            Cuenta.db("PUT", "${Cuenta.raiz}/stamps/${Cuenta.dispositivo}", ts.toString(), "print=silent")
            p.edit().putString("hash", hash).apply()
            runCatching { subirTiempo(c) }
            runCatching { portadas(c) }
            // Mi propio sello ya lo conozco: no hace falta bajarlo
            guardaVistos(c, vistos(c).put(Cuenta.dispositivo, ts))
            bien(c)
            true
        } catch (e: Exception) { mal(c, e); false }
    }

    /**
     * El nodo ligero para Azabache (el Diario y las barras): por juego y dispositivo, lo jugado en total y cada día, sin el resto de la ficha.
     * `tiempo/<kid>/<dispositivo>` = {t, k, al, m, p, total, days, fin}. Solo se escribe lo que ha cambiado desde la última vez; lo de un juego
     * que ya no está (o que se unió a otro) se borra.
     */
    fun subirTiempo(c: Context) {
        if (!Cuenta.vinculada) return
        val p = prefs(c)
        val dev = Cuenta.dispositivo
        val cambios = JSONObject()
        val vivos = HashSet<String>()
        for (j in Biblioteca.juegos.toList()) {
            if (j.msPropios <= 0L && j.diasPropios.isEmpty() && j.finalizado <= 0L) continue
            val kid = NubeP.kid(j); vivos += kid
            val nodo = JSONObject().put("t", j.titulo).put("k", j.clave).put("m", menuDe(j).name.lowercase()).put("total", j.msPropios).put("fin", j.finalizado)
                .put("days", JSONObject().also { d -> j.diasPropios.entries.sortedByDescending { it.key }.take(400).forEach { (k, v) -> d.put(k, v) } })
            if (j.plataforma.isNotBlank()) nodo.put("p", j.plataforma)
            if (j.alias.isNotEmpty()) nodo.put("al", org.json.JSONArray().also { a -> j.alias.forEach { a.put(it) } })
            val h = nodo.toString().hashCode().toString()
            if (p.getString("tp:$kid", "") == h) continue
            cambios.put("$kid/$dev", nodo)
            p.edit().putString("tp:$kid", h).apply()
        }
        // Lo que este dispositivo subió de juegos que ya no están, se quita
        for (k in p.all.keys.filter { it.startsWith("tp:") }) {
            val kid = k.removePrefix("tp:")
            if (kid in vivos) continue
            cambios.put("$kid/$dev", JSONObject.NULL); p.edit().remove(k).apply()
        }
        if (cambios.length() > 0) Cuenta.db("PATCH", "${Cuenta.raiz}/tiempo", cambios.toString(), "print=silent")
    }

    /** Las portadas que elegiste: van a tu kDrive (si está conectado), como las imágenes de Atrio. Errores aparte: no estropean la sincronización. */
    private fun portadas(c: Context) { if (KDrive.on(c)) { runCatching { NubeP.portadas(c) }; runCatching { NubeP.imagenes(c) }; runCatching { SubirRomsWorker.programar(c) } } }

    /** Baja y mezcla lo nuevo de tus otros dispositivos. Devuelve true si ha cambiado algo aquí. */
    suspend fun bajar(c: Context): Boolean = candado.withLock {
        if (!Cuenta.vinculada) return@withLock false
        try {
            val t = Cuenta.db("GET", "${Cuenta.raiz}/stamps", null)
            val sellos = t?.let { JSONObject(it) }
            val vistas = vistos(c)
            val ahora = System.currentTimeMillis()
            val nuevos = ArrayList<Blob>()
            val vistosNuevos = JSONObject(vistas.toString())
            if (sellos != null) for (dev in sellos.keys()) {
                val ts = sellos.optLong(dev, 0L)
                if (dev == Cuenta.dispositivo || ts <= vistas.optLong(dev, 0L) || ahora - ts > Cuenta.STALE) continue
                val raw = Cuenta.db("GET", "${Cuenta.raiz}/dev/$dev/data", null) ?: continue
                val datos = JSONTokener(raw).nextValue() as? String ?: continue
                Mezcla.deJson(datos)?.let { nuevos += Blob(dev, it.juegos, it.borrados, it.pantalla, it.pBorrados, it.ajustes, it.sagas, it.sagasBorradas, it.inicio) }
                vistosNuevos.put(dev, ts)
            }
            var cambio = false
            // La apariencia (tema, fondo, acento, colores a mano, letras…) más reciente de tus otros Carmesís
            nuevos.mapNotNull { b -> b.ajustes.takeIf { it.isNotBlank() }?.let { runCatching { JSONObject(it) }.getOrNull() } }.maxByOrNull { it.optLong("at") }
                ?.let { if (Skin.aplicarAjustes(c, it)) cambio = true }
            if (nuevos.isNotEmpty()) {
                val r = Mezcla.mezclar(Biblioteca.juegos.toList(), HashMap(Biblioteca.borrados), nuevos, Cuenta.dispositivo)
                if (r.cambio) { Biblioteca.aplicarMezcla(r.juegos, r.borrados); cambio = true }
                val rp = MezclaPantalla.mezclar(Pantalla.items.toList(), HashMap(Pantalla.borrados), nuevos.map { it.pantalla to it.pBorrados })
                if (rp.cambio) { Pantalla.aplicarMezcla(rp.items, rp.borrados); cambio = true }
                val rs = Mezcla.mezclarSagas(Sagas.lista.toList(), HashMap(Sagas.borradas), nuevos, Cuenta.dispositivo)
                if (rs.cambio) { Sagas.aplicarMezcla(rs.sagas, rs.borradas); cambio = true }
                // La portada (destacado, estantes, elegidos): la más reciente de tus otros Carmesís
                nuevos.map { it.inicio }.filter { it.isNotBlank() }.forEach { if (Inicio.aplicarRemoto(it)) cambio = true }
            }
            guardaVistos(c, vistosNuevos)
            bien(c)
            cambio
        } catch (e: Exception) { mal(c, e); false }
    }

    /** Al vincular una cuenta (o a mano, «Sincronizar ahora»): baja lo de los demás y sube lo de este dispositivo. */
    suspend fun sincronizar(c: Context, desdeCero: Boolean = false): Boolean {
        if (desdeCero) olvidarVistos()
        bajar(c)
        val ok = subir(c, forzar = true)
        portadas(c)
        return ok
    }

    /** Varias avisos de la escucha seguidos se juntan en una sola bajada. */
    fun bajarPronto() {
        val c = app ?: return
        if (!Cuenta.vinculada || !bajando.compareAndSet(false, true)) return
        scope.launch {
            try { delay(150); bajar(c) } finally { bajando.set(false) }
        }
    }

    /** Mientras Carmesí se ve: escucha tu cuenta en directo (lo que suba otro dispositivo se baja al momento). */
    fun escuchar(c: Context) {
        iniciar(c)
        if (!Cuenta.vinculada) { parar(); return }
        if (escucha != null) return
        // Al volver a Carmesí: se mira lo que haya pasado mientras tanto
        bajarPronto()
        val f = FbStream("portico", { renovar -> Cuenta.urlFlujo("${Cuenta.raiz}/stamps", renovar) }, { _, _ -> bajarPronto() }, { })
        escucha = f
        f.start(scope)
    }

    fun parar() { escucha?.stop(); escucha = null }

    private fun bien(c: Context) {
        fallos = 0
        val ahora = System.currentTimeMillis()
        ultima = ahora; prefs(c).edit().putLong("ultima", ahora).apply()
        estado = "Sincronizado"
    }

    private fun mal(c: Context, e: Exception) {
        fallos++
        estado = (e.message ?: "sin conexión").let { if (e is java.io.IOException) "Sin conexión: se reintenta solo." else it }
        // Se reintenta solo: 5 s, 15 s, 45 s… hasta 5 minutos
        if (fallos <= 8 && Cuenta.vinculada) cambio(c, minOf(300_000L, 5_000L * Math.pow(3.0, (fallos - 1).toDouble()).toLong()))
    }
}
