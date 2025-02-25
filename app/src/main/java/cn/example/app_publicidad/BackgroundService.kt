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

class BackgroundService : Service() {

    private lateinit var inactivityDetector: InactivityDetector


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
        return START_STICKY
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
}