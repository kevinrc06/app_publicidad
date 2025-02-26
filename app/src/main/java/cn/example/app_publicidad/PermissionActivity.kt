package cn.example.app_publicidad

import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class PermissionActivity : AppCompatActivity()  {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasUsageStatsPermission()) {
            startBackgroundService()
            finish()
        } else {
            showPermissionDialog()
        }
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


    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso Requerido")
            .setMessage("Esta aplicación necesita acceso a estadísticas de uso para detectar inactividad y mostrar publicidad.")
            .setPositiveButton("Conceder Permiso") { _, _ -> openUsageAccessSettings() }
            .setNegativeButton("Cancelar") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }


    private fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        if (hasUsageStatsPermission()) {
            startBackgroundService()
            finish()
        }
    }

/*    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPause() {
        super.onPause()
        if (hasUsageStatsPermission()) {
            startBackgroundService()
            finish()
        }
    }*/

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startBackgroundService() {
        val serviceIntent = Intent(this, BackgroundService::class.java)
        startForegroundService(serviceIntent)
    }
}