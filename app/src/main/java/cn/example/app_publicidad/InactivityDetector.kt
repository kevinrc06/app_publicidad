package cn.example.app_publicidad

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

class InactivityDetector(private val context: Context, private val listener: () -> Unit) {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 5000L // Revisar cada 5 segundos
    private val inactivityThreshold = 30000L // 30 segundos sin apps activas
    private var lastActiveTime = System.currentTimeMillis()

    private val checkRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            val foregroundApp = getForegroundApp()

            if (foregroundApp == null) {
                if ((now - lastActiveTime) > inactivityThreshold) {
                    Log.d("InactivityDetector", "Usuario inactivo, lanzando publicidad...")
                    listener.invoke()
                }
            } else {
                lastActiveTime = now // Reiniciar contador si hay una app en primer plano
            }

            handler.postDelayed(this, checkInterval)
        }
    }

    fun startMonitoring() {
        Log.d("InactivityDetector", "Iniciando monitoreo de inactividad...")
        handler.postDelayed(checkRunnable, checkInterval)
    }

    fun stopMonitoring() {
        handler.removeCallbacks(checkRunnable)
    }

    private fun getForegroundApp(): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, time - 10000, time
        )

        val topApp = stats?.maxByOrNull { it.lastTimeUsed }?.packageName

        Log.d("InactivityDetector", "App en primer plano: $topApp")

        return if (topApp == context.packageName) null else topApp
    }
}