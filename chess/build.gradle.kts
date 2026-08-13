plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.troxzy.trxchess.chess"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api(project(":core:common")); testImplementation(libs.junit) }
