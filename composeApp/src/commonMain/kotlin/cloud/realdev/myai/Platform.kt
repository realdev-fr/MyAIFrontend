package cloud.realdev.myai

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform