package com.example.savvyclub.data.model

data class Comrade(
    val uid: String? = null,
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "", // может быть url или "res:123"
    val isOnline: Boolean = false
)
