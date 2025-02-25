package cn.example.app_publicidad

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View

class PublicidadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publicidad)
        Log.d("PublicidadActivity", "Se mostró correctamente.")

        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnTouchListener { _, _ ->
            Log.d("PublicidadActivity", "Pantalla tocada, cerrando publicidad...")
            finish()
            true
        }
    }
}