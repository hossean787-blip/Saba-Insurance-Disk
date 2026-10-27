plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "ir.saba.insurancedisk"
    compileSdk = 35
    defaultConfig {
        applicationId = "ir.saba.insurancedisk"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0"
    }
    signingConfigs {
        create("stable") {
            val storeFilePath = System.getenv("SABA_KEYSTORE_PATH")
            if (!storeFilePath.isNullOrBlank()) {
                storeFile = file(storeFilePath)
                storePassword = System.getenv("SABA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SABA_KEY_ALIAS")
                keyPassword = System.getenv("SABA_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("debug") {
            if (!System.getenv("SABA_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("stable")
            }
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
