package com.example.techfeed.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_articles")
data class Article(
    @PrimaryKey val id: String,
    val source: String,
    val category: String,
    val title: String,
    val summary: String,
    val time: String,
    val url: String = id
)
