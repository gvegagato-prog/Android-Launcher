package com.gonzalo.portico

import android.content.Context
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Copiar la apariencia de Atrio. Atrio guarda su apariencia (tema, fondo, acento, colores a mano, letras y tamaño del texto) y la sube a tu
 * cuenta junto con sus datos (/users/<uid>/dev/<dispositivo>/data → "ajustes" → "skin"). Pórtico lee de ahí lo más reciente, enseña qué tiene
 * Atrio y copia lo que marques. No hace falta que Atrio esté instalado en este móvil; sí la cuenta vinculada. Solo se lee: no se cambia nada en Atrio.
 */
object CopiaAtrio {
    class Ajustes(val at: Long, val skin: JSONObject)
    class Fila(val id: String, val titulo: String, val valor: String)

    /** Lo último que Atrio subió a tu cuenta (null si no hay nada de Atrio). Bloqueante (red). */
    fun leer(): Ajustes? {
        val st = Cuenta.db("GET", "/users/${Cuenta.uid}/stamps", null)?.let { JSONObject(it) } ?: return null
        val devs = st.keys().asSequence().map { it to st.optLong(it) }.sortedByDescending { it.second }.take(4).toList()
        var mejor: Ajustes? = null
        for ((dev, _) in devs) {
            val raw = Cuenta.db("GET", "/users/${Cuenta.uid}/dev/$dev/data", null) ?: continue
            val datos = (JSONTokener(raw).nextValue() as? String) ?: continue
            val aj = runCatching { JSONObject(datos).optJSONObject("ajustes") }.getOrNull() ?: continue
            val skin = aj.optJSONObject("skin") ?: continue
            if (skin.length() == 0) continue
            val a = Ajustes(aj.optLong("at"), skin)
            if (mejor == null || a.at > mejor.at) mejor = a
        }
        return mejor
    }

    private fun largo(o: JSONObject, k: String): Long = if (o.has(k)) (o.opt(k) as? Number)?.toLong() ?: 0L else 0L
    private fun hex(c: Long) = "#%06X".format(c and 0xFFFFFF)

    /** Qué tiene Atrio, en filas que se pueden copiar (solo las que Atrio tiene). */
    fun filas(a: Ajustes): List<Fila> {
        val s = a.skin
        val out = ArrayList<Fila>()
        if (s.has("theme31")) out += Fila("tema", "Tema", when (s.optInt("theme31")) { 1 -> "Claro"; 2 -> "Oscuro"; else -> "Como el móvil" })
        out += Fila("fondo", "Fondo", FONDOS.firstOrNull { it.id == s.optString("skin_fondo") }?.name ?: "Papel")
        out += Fila("acento", "Acento", ACCENT_COLORS.firstOrNull { it.id == s.optString("skin_accent_id") }?.name ?: "Rojo")
        val f = largo(s, "skin_fondo_propio"); val t = largo(s, "skin_tinta_propia"); val c = largo(s, "skin_acento_propio")
        if (f != 0L || t != 0L || c != 0L) out += Fila("manera", "Colores a tu manera", listOfNotNull(if (f != 0L) "fondo ${hex(f)}" else null, if (t != 0L) "texto ${hex(t)}" else null, if (c != 0L) "acento ${hex(c)}" else null).joinToString(" · "))
        val lt = s.optString("skin_letra_titulos"); val lx = s.optString("skin_letra_texto")
        out += Fila("letras", "Letras", "títulos: ${lt.ifEmpty { "la de Atrio" }} · texto: ${lx.ifEmpty { "la de Atrio" }}")
        if (s.has("skin_escala")) out += Fila("tamano", "Tamaño del texto", Skin.ESCALAS_NOMBRES[s.optInt("skin_escala", 100)] ?: "${s.optInt("skin_escala")} %")
        return out
    }

    /** Copia a Pórtico lo marcado en [ids]. Pórtico deja de seguir los colores de Atrio en directo (ahora tiene los suyos, iguales). */
    fun aplicar(ctx: Context, a: Ajustes, ids: Set<String>) {
        val s = a.skin
        if ("tema" in ids && s.has("theme31")) Modo.eleccion = s.optInt("theme31").coerceIn(0, 2)
        if ("fondo" in ids) Skin.fondoId = s.optString("skin_fondo").takeIf { id -> FONDOS.any { it.id == id } } ?: ""
        if ("acento" in ids) Skin.accentId = s.optString("skin_accent_id").takeIf { id -> ACCENT_COLORS.any { it.id == id } } ?: "paleta"
        if ("manera" in ids) { Skin.fondoPropio = largo(s, "skin_fondo_propio"); Skin.tintaPropia = largo(s, "skin_tinta_propia"); Skin.acentoPropio = largo(s, "skin_acento_propio") }
        if ("letras" in ids) { Skin.display = s.optString("skin_letra_titulos").ifBlank { "Atrio" }; Skin.body = s.optString("skin_letra_texto").ifBlank { "Atrio" } }
        if ("tamano" in ids && s.has("skin_escala")) Skin.escala = s.optInt("skin_escala", 100).coerceIn(80, 140)
        if (ids.any { it in setOf("fondo", "acento", "tema", "manera") }) { Skin.origenAtrio = false; Skin.invertido = false }
        Skin.guardar(ctx)
    }
}
