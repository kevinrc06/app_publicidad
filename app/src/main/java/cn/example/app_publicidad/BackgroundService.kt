package cn.example.app_publicidad

import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import android.util.Base64
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

class BackgroundService : Service() {

    private lateinit var inactivityDetector: InactivityDetector
    val imagenesBase64 = mutableListOf<String>()


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()

        if (hasUsageStatsPermission()) {
            Log.d("BackgroundService", "Permiso concedido, iniciando monitoreo de inactividad...")
            inactivityDetector = InactivityDetector(this) {
                showPublicidadActivity()
            }
            inactivityDetector.startMonitoring()
        } else {
            Log.d("BackgroundService", "Permiso no concedido, inactividad no puede ser detectada.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        inactivityDetector.stopMonitoring()
    }
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun showPublicidadActivity() {
        Log.d("BackgroundService", "Intentando mostrar PublicidadActivity...")

        Handler(Looper.getMainLooper()).post {
            val intent = Intent(this, PublicidadActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
            Log.d("BackgroundService", "PublicidadActivity debería haberse mostrado.")
        }
    }


    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "background_channel")
            .setContentTitle("Servicio en segundo plano")
            .setContentText("Ejecutando tareas en segundo plano")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("MiAppPrefs", Context.MODE_PRIVATE)
        val ultimaFecha = prefs.getString("ultima_fecha", "")

        val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fechaActual = formatoFecha.format(Date())

        if (fechaActual != ultimaFecha) {
            Log.d("Servicio", "Día cambiado, iniciando nueva descarga.")

            eliminarArchivosAnteriores()
            Thread {
                val url = "http://10.173.51.208:5000/download"
                val archivoZip = descargarZip(url, "imagenes.zip", this)

                archivoZip?.let {
                    descomprimirZip(it)
                }

                prefs.edit().putString("ultima_fecha", fechaActual).apply()
            }.start()
        } else {
            Log.d("Servicio", "Ya se realizó la descarga hoy, no se ejecutará nuevamente.")
        }

        return START_STICKY
    }

    private fun eliminarArchivosAnteriores() {
        val directorioBase = getDir("MisImagenes", Context.MODE_PRIVATE)

        // Borrar el archivo ZIP anterior si existe
        val archivoZip = File(directorioBase, "imagenes.zip")
        if (archivoZip.exists()) {
            archivoZip.delete()
            Log.d("EliminarArchivos", "Archivo ZIP eliminado: ${archivoZip.absolutePath}")
        }

        // Borrar la carpeta de imágenes descomprimidas si existe
        val carpetaImagenes = File(directorioBase, "ImagenesDescomprimidas")
        if (carpetaImagenes.exists()) {
            carpetaImagenes.deleteRecursively()
            Log.d("EliminarArchivos", "Carpeta de imágenes eliminada: ${carpetaImagenes.absolutePath}")
        }
    }



    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "background_channel",
                "Servicio en segundo plano",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun descargarZip(url: String, nombreArchivo: String, context: Context): File? {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("DescargaZip", "Error en la descarga: ${connection.responseCode}")
                return null
            }

            val directorio = context.getDir("MisImagenes", Context.MODE_PRIVATE)
            if (!directorio.exists()) directorio.mkdirs()

            val archivoZip = File(directorio, nombreArchivo)

            val inputStream: InputStream = connection.inputStream
            val outputStream = FileOutputStream(archivoZip)

            val buffer = ByteArray(1024)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.close()
            inputStream.close()
            connection.disconnect()

            Log.d("DescargaZip", "Descarga completa: ${archivoZip.absolutePath}")
            return archivoZip

        } catch (e: Exception) {
            Log.e("DescargaZip", "Error: ${e.message}")
            return null
        }
    }


    private fun descomprimirZip(archivoZip: File) {
        val destino = File(archivoZip.parent, "ImagenesDescomprimidas")
        if (!destino.exists()) destino.mkdirs()

        val imagenesProcesadas = mutableSetOf<String>()

        try {
            ZipInputStream(FileInputStream(archivoZip)).use { zipStream ->
                var entry = zipStream.nextEntry
                val nombresProcesados = mutableSetOf<String>()

                while (entry != null) {
                    if (!entry.isDirectory && esImagen(entry.name) && nombresProcesados.add(entry.name)) {
                        val archivoSalida = File(destino, entry.name)
                        Log.d("DescomprimirZip", "Procesando imagen: ${entry.name}")

                        FileOutputStream(archivoSalida).use { output ->
                            zipStream.copyTo(output)
                        }

                        val base64String = convertirImagenABase64(archivoSalida)

                        if (!imagenesProcesadas.contains(base64String)) {
                            imagenesBase64.add(base64String)
                            imagenesProcesadas.add(base64String)
                        }
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
            Log.d("ImagenesBase64", "Imágenes convertidas: ${imagenesBase64.size}")
            Log.d("DescomprimirZip", "Descompresión completa en: ${destino.absolutePath}")
            Log.d("ImagenesBase64", "Lista de imágenes en Base64:")
            imagenesBase64.forEachIndexed { index, base64 ->
                Log.d(
                    "ImagenesBase64",
                    "[$index] ${base64.take(50)}..."
                ) // Mostramos solo los primeros 50 caracteres
            }

        } catch (e: Exception) {
            Log.e("DescomprimirZip", "Error al descomprimir: ${e.message}")
        }
    }





    fun convertirImagenABase64(archivo: File): String {
        return try {
            val bytes = archivo.readBytes()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("Base64", "Error al convertir imagen: ${e.message}")
            ""
        }
    }


    fun esImagen(nombreArchivo: String): Boolean {
        val extensionesValidas = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
        val extension = nombreArchivo.substringAfterLast('.', "").lowercase()
        return extensionesValidas.contains(extension)
    }

}