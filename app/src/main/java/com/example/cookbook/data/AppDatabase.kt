package com.example.cookbook.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Receita::class, Ingrediente::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "cookbook.db"
            )
                // Se existir um banco de uma versão incompatível, recria o esquema
                // em vez de interromper a inicialização do aplicativo.
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
        }
    }
}
