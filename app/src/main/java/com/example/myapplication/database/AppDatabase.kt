package com.example.myapplication.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.Utilisateur


@Database(entities = arrayOf(Utilisateur::class), version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun utilisateurDao(): UtilisateurDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(
            context: Context
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "test_database"
                )
                    .build()

                println("first")
                INSTANCE = instance

                instance
            }
        }
    }
}