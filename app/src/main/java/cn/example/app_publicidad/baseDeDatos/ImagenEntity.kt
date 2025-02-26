package cn.example.app_publicidad.baseDeDatos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "imagenes")
data class ImagenEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val base64: String
)