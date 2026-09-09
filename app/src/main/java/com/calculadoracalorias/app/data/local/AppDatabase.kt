package com.calculadoracalorias.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.calculadoracalorias.app.data.local.dao.MealDao
import com.calculadoracalorias.app.data.local.dao.SupplementDao
import com.calculadoracalorias.app.data.local.dao.SupplementLogDao
import com.calculadoracalorias.app.data.local.entity.MealEntryEntity
import com.calculadoracalorias.app.data.local.entity.MealFoodItemEntity
import com.calculadoracalorias.app.data.local.entity.SupplementEntity
import com.calculadoracalorias.app.data.local.entity.SupplementLogEntity

@Database(
    entities = [
        MealEntryEntity::class,
        MealFoodItemEntity::class,
        SupplementLogEntity::class,
        SupplementEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao
    abstract fun supplementLogDao(): SupplementLogDao
    abstract fun supplementDao(): SupplementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calculadora_calorias_database"
                ).fallbackToDestructiveMigrationOnDowngrade().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
