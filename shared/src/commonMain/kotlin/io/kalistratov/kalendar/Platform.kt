package io.kalistratov.kalendar

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform