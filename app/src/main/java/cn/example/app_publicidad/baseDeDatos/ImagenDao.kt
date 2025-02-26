package cn.example.app_publicidad.baseDeDatos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
@Dao
interface ImagenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarImagenes(imagenes: List<ImagenEntity>)

    @Query("SELECT * FROM imagenes")
    suspend fun obtenerTodasLasImagenes(): List<ImagenEntity>

    @Query("SELECT COUNT(*) FROM imagenes")
    suspend fun obtenerCantidad(): Int

    @Query("DELETE FROM imagenes")
    suspend fun eliminarTodo()
}