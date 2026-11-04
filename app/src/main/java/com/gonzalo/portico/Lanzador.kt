package com.gonzalo.portico

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast

/** Cómo se eligió el comando de un juego. */
class Eleccion(val etiqueta: String, val plantilla: String, val resultado: Resultado)

object Lanzador {
    fun entorno(c: Context) = object : Entorno {
        val pm = c.packageManager
        override fun instalado(paquete: String): Boolean = try {
            pm.getPackageInfo(paquete, 0); true
        } catch (_: PackageManager.NameNotFoundException) { false }
        override val internos = "/data/data"
        override val externos: String = Environment.getExternalStorageDirectory().absolutePath
    }

    fun comandosDe(sistema: String): List<Pair<String, String>> =
        Catalogo.sistema(sistema)?.comandos.orEmpty().filter { Comando.soportado(it.second) }

    fun rom(j: Juego) = RomRef(j.uri, j.ruta.ifBlank { null })

    /** Resultado de cada comando del sistema para esta ROM (para enseñar cuáles se pueden usar ahora). */
    fun evaluar(c: Context, j: Juego): List<Eleccion> {
        val env = entorno(c); val r = rom(j)
        return comandosDe(j.sistema).map { (l, t) -> Eleccion(l, t, Comando.resolver(t, r, env)) }
    }

    /**
     * El comando que se usará: el elegido para el juego, si no el elegido para el sistema, y si no uno automático:
     * primero los emuladores propios (sin núcleos que descargar), luego RetroArch, entre los que estén instalados.
     */
    fun elegir(c: Context, j: Juego): Eleccion? {
        val todos = evaluar(c, j)
        if (todos.isEmpty()) return null
        val pedido = j.emulador.ifBlank { Ajustes.emuladorPorSistema[j.sistema].orEmpty() }
        if (pedido.isNotBlank()) todos.firstOrNull { it.etiqueta == pedido }?.let { return it }
        return todos.filter { it.resultado is Resultado.Ok && !Comando.esRetroArch(it.plantilla) }.firstOrNull()
            ?: todos.firstOrNull { it.resultado is Resultado.Ok }
            ?: todos.firstOrNull { !Comando.esRetroArch(it.plantilla) } ?: todos.first()
    }

    fun aIntent(s: Especificacion): Intent {
        val i = Intent()
        s.clase?.let { i.setClassName(s.paquete, it) } ?: i.setPackage(s.paquete)
        s.accion?.let { i.action = it }
        val d = s.datos?.let { Uri.parse(it) }
        if (d != null && s.mime != null) i.setDataAndType(d, s.mime) else if (d != null) i.data = d else if (s.mime != null) i.type = s.mime
        s.categorias.forEach { i.addCategory(it) }
        for (e in s.extras) when (val v = e.valor) { is Boolean -> i.putExtra(e.clave, v); is Int -> i.putExtra(e.clave, v); else -> i.putExtra(e.clave, v.toString()) }
        var f = Intent.FLAG_ACTIVITY_NEW_TASK
        if (s.limpiarTarea) f = f or Intent.FLAG_ACTIVITY_CLEAR_TASK
        if (s.limpiarArriba) f = f or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (s.conceder != null) {
            f = f or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            // El permiso de lectura vale también para el documento que va dentro de un extra o dentro de los datos
            i.clipData = ClipData.newRawUri("rom", Uri.parse(s.conceder))
        }
        i.addFlags(f)
        return i
    }

    /** Lanza el juego. Devuelve null si salió bien o el texto del problema. */
    fun lanzar(c: Context, j: Juego, alTerminar: () -> Unit = {}): String? {
        // Con guardados en la nube: antes de abrir el emulador se mira si hay uno más reciente en otro móvil (en segundo plano; se abre al acabar)
        if (j.tipo == Tipo.ROM && NubeP.activo(c, j)) {
            val e = elegir(c, j)
            if (e != null && e.resultado is Resultado.Ok) {
                val app = c.applicationContext
                val principal = android.os.Handler(android.os.Looper.getMainLooper())
                kotlin.concurrent.thread {
                    val texto = runCatching { NubeP.carpetaReal(app, NubeP.carpetas[j.sistema]!!)?.let { NubeP.antes(app, j, it) } }.getOrNull()
                    principal.post {
                        texto?.let { aviso(app, it) }
                        lanzarYa(c, j)?.let { aviso(app, it) }
                        alTerminar()
                    }
                }
                return null
            }
        }
        return lanzarYa(c, j).also { alTerminar() }
    }

    private fun lanzarYa(c: Context, j: Juego): String? {
        val error: String? = try {
            when (j.tipo) {
                Tipo.ROM -> {
                    if (j.enOtroDispositivo) return "Esta ROM está en otro de tus dispositivos: añádela aquí (mismo sistema y mismo nombre de archivo) y se unirá sola."
                    val e = elegir(c, j) ?: return "Carmesí no sabe lanzar «${j.sistema}»"
                    when (val r = e.resultado) {
                        is Resultado.Ok -> { c.startActivity(aIntent(r.spec)); null }
                        is Resultado.Falta -> "${e.etiqueta}: ${r.motivo}"
                    }
                }
                Tipo.APP -> {
                    val i = c.packageManager.getLaunchIntentForPackage(j.paquete)
                    if (i == null) "Esa app ya no está instalada" else { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(i); null }
                }
                Tipo.WEB -> { c.startActivity(WebJuegoActivity.intent(c, j)); null }
                Tipo.FICHA -> return "«${j.titulo}» no se juega desde el móvil" + (if (j.plataforma.isNotBlank()) " (${j.plataforma})" else "") + ". Si tienes su ROM, su app o su web, vincúlala desde la ficha."
                Tipo.PC -> return "«${j.titulo}» es del PC: desde su ficha, «Jugar en el PC» o «Streaming»."
            }
        } catch (e: ActivityNotFoundException) { "No se pudo abrir: ¿sigue instalado el emulador?"
        } catch (e: SecurityException) { "Android no deja abrir esto: ${e.message}" }
        if (error == null) Sesion.empezar(c, j)
        return error
    }

    fun aviso(c: Context, texto: String) = Toast.makeText(c, texto, Toast.LENGTH_LONG).show()

    /** Abre la ficha de un emulador en la tienda (Play Store, o el navegador si no hay). */
    fun tienda(c: Context, paquete: String) {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$paquete")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { c.startActivity(i) } catch (_: ActivityNotFoundException) {
            c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$paquete")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

/**
 * Tiempo de juego aproximado. Cuando lanzas un juego (ROM o app) se apunta la hora; cuando vuelves a Carmesí, lo que ha pasado
 * se suma al juego (con un máximo de 12 h por sesión; menos de 20 s no cuenta). Es una aproximación: Android no dice cuándo
 * cierras un emulador. Los juegos web se miden dentro de su propia pantalla.
 */
object Sesion {
    private fun p(c: Context) = c.getSharedPreferences("portico_sesion", Context.MODE_PRIVATE)

    /** Reparte un tiempo de juego entre los días que abarca (una sesión que cruza la medianoche cuenta en los dos). */
    fun repartir(fin: Long, ms: Long, zona: java.util.TimeZone = java.util.TimeZone.getDefault()): Map<String, Long> {
        val out = LinkedHashMap<String, Long>()
        val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).apply { timeZone = zona }
        var hasta = fin; var resto = ms
        while (resto > 0) {
            val cal = java.util.Calendar.getInstance(zona).apply { timeInMillis = hasta - 1 }
            val inicioDia = cal.apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis
            val enDia = minOf(resto, hasta - inicioDia)
            val k = f.format(java.util.Date(hasta - 1))
            out[k] = (out[k] ?: 0L) + enDia
            resto -= enDia; hasta = inicioDia
        }
        return out
    }

    fun conDias(j: Juego, fin: Long, ms: Long): Map<String, Long> {
        val d = HashMap(j.diasPropios)
        repartir(fin, ms).forEach { (k, v) -> d[k] = (d[k] ?: 0L) + v }
        return d.entries.sortedByDescending { it.key }.take(400).associate { it.key to it.value }
    }

    fun empezar(c: Context, j: Juego) {
        val k = if (j.estado == Estado.PENDIENTE) j.conEstado(Estado.JUGANDO) else j
        Biblioteca.poner(k.copy(ultima = System.currentTimeMillis(), veces = j.veces + 1, vecesPropias = j.vecesPropias + 1))
        if (j.tipo != Tipo.WEB) p(c).edit().putString("id", j.id).putLong("desde", System.currentTimeMillis()).apply()
    }

    /** Al volver a Carmesí: suma el tiempo de la sesión que había abierta. */
    fun cerrar(c: Context) {
        val p = p(c); val id = p.getString("id", null) ?: return
        val desde = p.getLong("desde", 0L); p.edit().clear().apply()
        val ms = (System.currentTimeMillis() - desde).coerceAtMost(12L * 3600_000)
        if (ms >= 20_000) Biblioteca.por(id)?.let { Biblioteca.poner(it.copy(msJugados = it.msJugados + ms, msPropios = it.msPropios + ms, diasPropios = conDias(it, System.currentTimeMillis(), ms))) }
        // Volver de jugar: si el guardado de este móvil ha cambiado, se sube a la nube
        Biblioteca.por(id)?.takeIf { NubeP.activo(c, it) }?.let { j ->
            val app = c.applicationContext
            kotlin.concurrent.thread { runCatching { NubeP.carpetaReal(app, NubeP.carpetas[j.sistema]!!)?.let { NubeP.despues(app, j, it) } } }
        }
    }

    fun sumar(j: Juego, ms: Long) { if (ms >= 5_000) Biblioteca.por(j.id)?.let { Biblioteca.poner(it.copy(msJugados = it.msJugados + ms, msPropios = it.msPropios + ms, diasPropios = conDias(it, System.currentTimeMillis(), ms))) } }
}
