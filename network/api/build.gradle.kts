plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.troxzy.trxchess.network.api"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { implementation(project(":core:common")) }
