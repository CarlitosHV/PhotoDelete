package com.hardbug.photodelete

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform