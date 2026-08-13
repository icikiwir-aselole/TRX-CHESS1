plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.troxzy.trxchess.engine.uci"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api(project(":engine:api")); implementation(libs.coroutines); testImplementation(libs.junit) }
