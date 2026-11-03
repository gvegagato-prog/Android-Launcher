package com.gonzalo.portico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.SideEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        iniciar(this)
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
                PorticoApp()
            }
        }
    }

    /** Volver a Pórtico después de jugar: se apunta el tiempo que has estado fuera. */
    override fun onStart() {
        super.onStart(); Sesion.cerrar(this)
        // Los colores de Atrio pueden haber cambiado mientras estabas en ella: se vuelven a leer
        val c = applicationContext
        kotlin.concurrent.thread { TemaAtrio.refrescar(c) }
    }
}
