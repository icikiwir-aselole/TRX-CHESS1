plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.troxzy.trxchess.engine.nativeengine"
    compileSdk = 37
    defaultConfig { minSdk = 24; externalNativeBuild { cmake { cppFlags += listOf("-std=c++20", "-O3") } } }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt") } }
}
dependencies { api(project(":engine:api")); implementation(project(":engine:uci")) }
