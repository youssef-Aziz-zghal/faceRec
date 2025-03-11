package com.example.myapplication.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "utilisateur")
data class Utilisateur(
    @PrimaryKey(autoGenerate = true) var id: Int=0,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "image") var image : ByteArray?=null
)