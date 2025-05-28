package com.example.myapplicationnnn

data class Skin(
    val id: Int,
    val name: String,
    val imageResource: Int,
    val price: Int,
    val isUnlocked: Boolean = false
)