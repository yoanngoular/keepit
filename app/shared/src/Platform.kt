package com.ygoular.keepit

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
