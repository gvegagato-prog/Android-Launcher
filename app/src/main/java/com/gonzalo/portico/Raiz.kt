package com.gonzalo.portico

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/** Arranca los datos de la app (una vez). */
fun iniciar(c: Context) {
    Catalogo.cargar(c); Skin.cargar(c); Ajustes.cargar(c); NubeP.cargar(c); Biblioteca.cargar(c); Pantalla.cargar(c)
    TemaAtrio.cargarGuardado(c)
    Cuenta.cargar(c); SincroP.iniciar(c)
}

/** Dónde estás: la portada y sus secciones (se llega a ellas desde el menú de la portada, como en Atrio). */
enum class Destino { Portada, Jugando, Pendiente, Pasado, Abandonado, Todo, Sistemas, Ajustes }

/** La app: portada de revista y sus secciones, la ficha de un juego por encima y la hoja de «añadir». */
@Composable
fun PorticoApp(fichaPedida: String? = null, alAbrirPedida: () -> Unit = {}) {
    val ctx = LocalContext.current
    var destino by rememberSaveable { mutableIntStateOf(0) }
    var fichaId by rememberSaveable { mutableStateOf<String?>(null) }
    var anadir by rememberSaveable { mutableStateOf(false) }
    var tipoInicial by remember { mutableStateOf<Tipo?>(null) }
    val filtro = remember { FiltroBib() }
    val d = Destino.entries[destino]
    // Un widget pide abrir la ficha de un juego
    LaunchedEffect(fichaPedida) { if (fichaPedida != null && Biblioteca.por(fichaPedida) != null) { fichaId = fichaPedida; destino = 0 }; if (fichaPedida != null) alAbrirPedida() }

    fun jugar(j: Juego) { Lanzador.lanzar(ctx, j)?.let { Lanzador.aviso(ctx, it) } }
    fun abrirAnadir(t: Tipo?) { tipoInicial = t; anadir = true }
    fun ir(x: Destino) {
        when (x) {
            Destino.Jugando -> { filtro.limpiar(); filtro.estado = Estado.JUGANDO }
            Destino.Pendiente -> { filtro.limpiar(); filtro.estado = Estado.PENDIENTE }
            Destino.Pasado -> { filtro.limpiar(); filtro.estado = Estado.PASADO }
            Destino.Abandonado -> { filtro.limpiar(); filtro.estado = Estado.ABANDONADO }
            Destino.Todo -> filtro.limpiar()
            else -> {}
        }
        destino = x.ordinal
    }

    BackHandler(enabled = fichaId != null) { fichaId = null }
    AvisoActualizacion()
    BackHandler(enabled = fichaId == null && d != Destino.Portada) { destino = 0 }

    Box(Modifier.fillMaxSize().background(Paper)) {
        when (d) {
            Destino.Portada -> PantInicio(
                alAbrir = { fichaId = it.id }, alJugar = ::jugar, alAnadir = { abrirAnadir(it) },
                alIr = { ir(it) }, alBuscar = { ir(Destino.Todo); filtro.buscando = true }
            )
            Destino.Sistemas -> PantSistemas(alVolver = { destino = 0 }, alAnadirCarpeta = { abrirAnadir(null) })
            Destino.Ajustes -> PantEstilo(alVolver = { destino = 0 }, alSistemas = { destino = Destino.Sistemas.ordinal })
            else -> PantBiblioteca(filtro, alVolver = { destino = 0 }, alAbrir = { fichaId = it.id }, alJugar = ::jugar, alAnadir = { abrirAnadir(null) })
        }
        AnimatedVisibility(fichaId != null, enter = slideInVertically { it / 8 } + fadeIn(), exit = slideOutVertically { it / 8 } + fadeOut()) {
            fichaId?.let { id -> Ficha(id, alCerrar = { fichaId = null }, alJugar = ::jugar) }
        }
    }
    if (anadir) HojaAnadir(tipoInicial) { anadir = false }
}
