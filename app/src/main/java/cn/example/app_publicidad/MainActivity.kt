package cn.example.app_publicidad

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startActivity(Intent(this, PermissionActivity::class.java))
        Toast.makeText(this,"Iniciando servicio",Toast.LENGTH_LONG).show()
    }

    @Override
    override fun onResume() {
        super.onResume()
        Toast.makeText(this,"Servicio corriendo",Toast.LENGTH_SHORT).show()
        finish()
    }
}