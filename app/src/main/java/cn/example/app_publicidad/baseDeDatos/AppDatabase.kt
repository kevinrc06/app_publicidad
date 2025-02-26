package cn.example.app_publicidad.baseDeDatos
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ImagenEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imagenDao(): ImagenDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "imagenes_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}