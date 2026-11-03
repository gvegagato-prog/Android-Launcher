package com.gonzalo.portico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    /** La ficha que pide abrir un widget (se consume al abrirla). */
    private var fichaPedida by androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent); fichaPedida = intent.getStringExtra("ficha")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        iniciar(this)
        Actualizador.init(this)
        fichaPedida = intent?.getStringExtra("ficha")
        setContent {
            PorticoTheme {
                // Iconos de la barra de estado claros u oscuros según el fondo
                SideEffect {
                    val oscuro = Skin.tones.darkBg
                    enableEdgeToEdge(
                        statusBarStyle = if (oscuro) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                        navigationBarStyle = if (oscuro) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    )
                }
                PorticoApp(fichaPedida) { fichaPedida = null }
            }
        }
    }

    /** Volver a Pórtico después de jugar: se apunta el tiempo que has estado fuera. */
    override fun onStart() {
        super.onStart(); Sesion.cerrar(this)
        // Los colores de Atrio pueden haber cambiado mientras estabas en ella: se vuelven a leer
        val c = applicationContext
        kotlin.concurrent.thread { TemaAtrio.refrescar(c) }
        // Tu cuenta de Atrio, en directo mientras Pórtico se ve
        SincroP.escuchar(c)
        SubirRomsWorker.programar(c)
    }

    override fun onStop() { super.onStop(); SincroP.parar() }
}
