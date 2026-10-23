plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.yhan.tkgmekran"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yhan.tkgmekran"
        minSdk = 30          // Android 11+ (erişilebilirlik ekran görüntüsü API'si)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Sabit imza: her derlemede aynı anahtar → güncellemede kaldırıp yüklemek gerekmez
    signingConfigs {
        create("yhan") {
            storeFile = file("yhan.keystore")
            storePassword = "yhan2026"
            keyAlias = "yhan"
            keyPassword = "yhan2026"
        }
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("yhan") }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("yhan")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

base { archivesName.set("TKGM_EKRAN_ALINTI_YHAN-v1.0") }
