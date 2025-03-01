package cn.example.app_publicidad

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import cn.example.app_publicidad.baseDeDatos.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class PublicidadActivity : AppCompatActivity() {
    private lateinit var listaImagenesBase64: MutableList<String>
    private lateinit var imageViewPublicidad: ImageView
    private lateinit var listaHorarios: List<Pair<String, String>> // Lista de horarios asociados a imágenes
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publicidad)
        imageViewPublicidad = findViewById(R.id.imagenMostrar)
        Log.d("PublicidadActivity", "Se mostró correctamente.")

        listaImagenesBase64 = mutableListOf()
        listaHorarios = mutableListOf()
        obtenerImagenesDesdeBD()

        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnTouchListener { _, _ ->
            Log.d("PublicidadActivity", "Pantalla tocada, cerrando publicidad...")
            finish()
            true
        }
    }

    private fun obtenerImagenesDesdeBD() {
        val db = AppDatabase.getDatabase(this)
        val imagenDao = db.imagenDao()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val imagenes = imagenDao.obtenerTodasLasImagenes()
                listaImagenesBase64.clear()
                listaImagenesBase64.addAll(imagenes.map { it.base64 })
                listaHorarios = imagenes.map { it.horaInicio to it.horaFin }

                withContext(Dispatchers.Main) {
                    Log.d("PublicidadActivity", "Se cargaron ${listaImagenesBase64.size} imágenes desde la base de datos.")
                    iniciarActualizacionAutomatica()
                }
            } catch (e: Exception) {
                Log.e("PublicidadActivity", "Error al obtener imágenes: ${e.message}")
            }
        }
    }

    private fun iniciarActualizacionAutomatica() {
        actualizarImagenSegunHora()
        runnable = object : Runnable {
            override fun run() {
                actualizarImagenSegunHora()
                handler.postDelayed(this, 60 * 1000) // Verificar cada minuto
            }
        }
        handler.post(runnable!!)
    }

    private fun actualizarImagenSegunHora() {
        val horaActual = obtenerHoraActual()
        val index = listaHorarios.indexOfFirst { (inicio, fin) -> horaActual in inicio..fin }

        if (index != -1) {
            mostrarImagen(index)
        } else {
            Log.d("PublicidadActivity", "No hay imagen asignada para este horario.")
        }
    }

    private fun obtenerHoraActual(): String {
        val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formato.format(Date())
    }

    private fun mostrarImagen(posicion: Int) {
        if (posicion in listaImagenesBase64.indices) {
            val base64String = listaImagenesBase64[posicion]
            val bitmap = convertirBase64ABitmap(base64String)
            imageViewPublicidad.setImageBitmap(bitmap)
        }
    }

    private fun convertirBase64ABitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e("PublicidadActivity", "Error al decodificar imagen: ${e.message}")
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable!!)
    }
}
