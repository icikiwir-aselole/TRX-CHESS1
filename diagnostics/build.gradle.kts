plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.troxzy.trxchess.diagnostics"; compileSdk = 37; defaultConfig { minSdk = 24 } }
dependencies { api(project(":engine:api")); api(project(":core:common")); implementation(libs.androidx.core); testImplementation(libs.junit) }
