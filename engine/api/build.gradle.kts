plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.troxzy.trxchess.engine.api"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api(project(":chess")); api(project(":core:common")); api(libs.coroutines); testImplementation(libs.junit) }
