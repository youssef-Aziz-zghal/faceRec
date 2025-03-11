package com.example.myapplication.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.Utilisateur
import kotlinx.coroutines.flow.Flow


@Dao
interface UtilisateurDao {
    @Insert
    suspend fun insertAll(parks: List<Utilisateur>)

    @Query("SELECT * FROM utilisateur")
    suspend fun getAll(): List<Utilisateur>


    @Query("DELETE  FROM utilisateur")
    suspend fun deleteAll()

    @Query("SELECT * FROM utilisateur")
    fun getAllFlow(): Flow<List<Utilisateur>>

    @Query("SELECT * FROM utilisateur")
    fun getAllLiveData(): LiveData<List<Utilisateur>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(park: Utilisateur)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveNoSuspend(park: Utilisateur)



    @Delete
    suspend fun delete(parks:Utilisateur)

    @Delete
     fun deleteNoSuspend(parks:Utilisateur)




}