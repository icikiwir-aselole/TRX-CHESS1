import java.util.Properties

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

val keystoreProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(name: String, env: String): String? = keystoreProps.getProperty(name) ?: System.getenv(env)

android {
    namespace = "com.troxzy.trxchess"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.troxzy.trxchess"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        val path = secret("signing.store.path", "SIGNING_STORE_PATH")
        val pw = secret("signing.store.password", "SIGNING_STORE_PASSWORD")
        val alias = secret("signing.key.alias", "SIGNING_KEY_ALIAS")
        val kpw = secret("signing.key.password", "SIGNING_KEY_PASSWORD")
        if (path != null && pw != null && alias != null && kpw != null) {
            create("release") {
                storeFile = file(path)
                storePassword = pw
                keyAlias = alias
                keyPassword = kpw
            }
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        create("staging") { initWith(getByName("debug")); matchingFallbacks += listOf("debug") }
        release {
            isMinifyEnabled = true; isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    buildFeatures { buildConfig = true }
    testOptions {
        unitTests { isReturnDefaultValues = true }
    }
}
dependencies {
    implementation(project(":core:common")); implementation(project(":chess")); implementation(project(":engine:api")); implementation(project(":engine:uci")); implementation(project(":engine:native")); implementation(project(":analysis:coordinator")); implementation(project(":position:api")); implementation(project(":data")); implementation(project(":security")); implementation(project(":overlay")); implementation(project(":automation")); implementation(project(":ui"))
    implementation(project(":network:api")); implementation(project(":diagnostics")); implementation(project(":benchmark"))
    implementation(libs.androidx.activity); implementation(libs.androidx.core); implementation(libs.androidx.lifecycle); testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
