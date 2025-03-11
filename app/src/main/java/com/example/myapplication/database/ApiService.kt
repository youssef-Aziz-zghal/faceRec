package com.example.myapplication.database

import android.content.Context
import com.example.myapplication.data.Utilisateur
import kotlinx.coroutines.runBlocking


class ApiService(context: Context){

    private  val db: AppDatabase=AppDatabase.getDatabase(context)
    //  private val db=MyDataApplication.database //no need for context in the constructor
    private val utilisateurService=db.utilisateurDao()



   // fun rechercherUtilisateurApplicationmethod2()=MyDataApplication.database.utilisateurDao().getAllLiveData()




    suspend fun rechercherUtilisateur(): List<Utilisateur> {
        val liste = utilisateurService.getAll()
        return liste
    }

    fun rechercherUtilisteurFlow() = utilisateurService.getAllFlow()

    fun rechercherUtilisteurLiveData() = utilisateurService.getAllLiveData()




    fun rechercherUtilisateurBlocking(): List<Utilisateur> {
        val liste = runBlocking { utilisateurService.getAll() }
        return liste
    }

    suspend fun persitUtilisateur(utilisateur: Utilisateur) {
        utilisateurService.save(utilisateur)
    }

    fun persitUtilisateurNoSuspend(utilisateur: Utilisateur) {
        utilisateurService.saveNoSuspend(utilisateur)
    }


    suspend fun deleteUtilisateur(user: Utilisateur) {
        utilisateurService.delete(user)
    }

     fun deleteUtilisateurNoSuspend(user: Utilisateur) {
        utilisateurService.deleteNoSuspend(user)
    }

    fun deleteUtilisateurBlocking(user: Utilisateur) {
        runBlocking { utilisateurService.delete(user) }
    }

    suspend fun deleteAll() {
        utilisateurService.deleteAll()
    }

    suspend fun login(id: Int, name: String): Utilisateur? {
        return utilisateurService.getAll().find { it.id == id && it.name == name }

    }
}