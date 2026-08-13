plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.troxzy.trxchess.position.api"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api(project(":chess")) }
