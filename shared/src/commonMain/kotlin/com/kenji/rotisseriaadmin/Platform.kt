package com.kenji.rotisseriaadmin

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform