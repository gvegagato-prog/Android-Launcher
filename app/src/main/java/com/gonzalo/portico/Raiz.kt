package com.gonzalo.portico

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/** Arranca los datos de la app (una vez). */
fun iniciar(c: Context) {
    Catalogo.cargar(c); Skin.cargar(c); Ajustes.cargar(c); Biblioteca.cargar(c)
}

/** La app: cuatro destinos (Inicio, Juegos, Sistemas, Estilo), el «+» de añadir en el dock, y la ficha de un juego por encima. */
@Composable
fun PorticoApp() {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var fichaId by rememberSaveable { mutableStateOf<String?>(null) }
    var anadir by rememberSaveable { mutableStateOf(false) }
    var tipoInicial by remember { mutableStateOf<Tipo?>(null) }
    val filtro = remember { FiltroBib() }

    fun jugar(j: Juego) { Lanzador.lanzar(ctx, j)?.let { Lanzador.aviso(ctx, it) } }
    fun abrirAnadir(t: Tipo?) { tipoInicial = t; anadir = true }

    BackHandler(enabled = fichaId != null) { fichaId = null }
    BackHandler(enabled = fichaId == null && tab != 0) { tab = 0 }

    Box(Modifier.fillMaxSize().background(Paper)) {
        when (tab) {
            0 -> PantInicio(
                alAbrir = { fichaId = it.id }, alJugar = ::jugar, alAnadir = { abrirAnadir(it) },
                alVerSistema = { s, t -> filtro.limpiar(); filtro.sistema = s; filtro.tipo = if (s != null) Tipo.ROM else t; filtro.favoritos = s == null && t == null; tab = 1 },
                alEstilo = { tab = 3 }
            )
            1 -> PantBiblioteca(filtro, alAbrir = { fichaId = it.id }, alJugar = ::jugar, alAnadir = { abrirAnadir(null) })
            2 -> PantSistemas(alAnadirCarpeta = { abrirAnadir(null) })
            else -> PantEstilo()
        }
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) {
            Dock(DESTINOS, tab, { tab = it }, { abrirAnadir(null) })
        }
        AnimatedVisibility(fichaId != null, enter = slideInVertically { it / 6 } + fadeIn(), exit = slideOutVertically { it / 6 } + fadeOut()) {
            fichaId?.let { id -> Ficha(id, alCerrar = { fichaId = null }, alJugar = ::jugar) }
        }
    }
    if (anadir) HojaAnadir(tipoInicial) { anadir = false }
}
