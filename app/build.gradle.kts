plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val oreoAccessCode = providers.gradleProperty("oreoAccessCode")
    .orElse(providers.environmentVariable("OREO_ACCESS_CODE"))
    .orElse("OREO-2026")
    .get()
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.oreoexperience.notes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.oreoexperience.notes"
        minSdk = 26
        targetSdk = 34
        versionCode = 100
        versionName = "1.0.0 ORBETA"

        vectorDrawables { useSupportLibrary = true }
        buildConfigField("String", "ACCESS_CODE", "\"$oreoAccessCode\"")
    }

    // Firma estable de "release" sideload — checked-in en el repo. Esto NO
    // es para Play Store; sirve para que cada APK que Devin construya en
    // cualquier máquina se firme con la misma huella y el usuario pueda
    // **actualizar** la app sin desinstalarla primero. La clave es
    // pública (passwords visibles en el repo) por diseño — si en algún
    // momento la app va a Play Store hay que rotarla por una de release
    // real protegida.
    signingConfigs {
        create("oreo") {
            storeFile = file("release.keystore")
            storePassword = "oreoexperience"
            keyAlias = "oreoexperience"
            keyPassword = "oreoexperience"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("oreo")
        }
        debug {
            isDebuggable = true
            // Usamos el mismo signing config que release para que ambos
            // APKs sean intercambiables al momento de actualizar.
            signingConfig = signingConfigs.getByName("oreo")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.richeditor.compose)

    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
