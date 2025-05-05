package cn.example.app_publicidad

import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class PermissionActivity : AppCompatActivity()  {

    private var serviceInit : Boolean = false

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
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        if (hasUsageStatsPermission() && !serviceInit){
            startBackgroundService()
            serviceInit=true
            Log.d("permisos","entro por la primera validacion")
            finish()
        }else{
            checkPermissionLoop()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkPermissionLoop() {
        val handler = android.os.Handler(mainLooper)
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (hasUsageStatsPermission() && !serviceInit) {
                    startBackgroundService()
                    serviceInit=true
                    Log.d("permisos","entro por la segunda validacion")
                    finish()
                }else {
                    handler.postDelayed(this,2000)
                }
            }
        }, 4000)
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