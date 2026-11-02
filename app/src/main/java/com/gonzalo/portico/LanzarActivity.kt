package com.gonzalo.portico

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

/** Sin pantalla: la usan los accesos directos del móvil («Pokémon Esmeralda» en tu escritorio) para lanzar un juego derecho. */
class LanzarActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Biblioteca.cargar(this); Catalogo.cargar(this); Ajustes.cargar(this)
        val j = intent.getStringExtra("id")?.let { Biblioteca.por(it) }
        if (j == null) Lanzador.aviso(this, "Ese juego ya no está en Pórtico")
        else Lanzador.lanzar(this, j)?.let { Lanzador.aviso(this, it) }
        finish()
    }

    companion object {
        const val ACCION = "com.gonzalo.portico.JUGAR"
        fun intent(c: Context, id: String) = Intent(c, LanzarActivity::class.java).setAction(ACCION).putExtra("id", id)
    }
}
