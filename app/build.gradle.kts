import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// google-services plugin se primenjuje samo ako postoji app/google-services.json.
// Tako build tima ne puca dok se Firebase ne podesi; FCM tada radi kao no-op (isto kao backend).
if (rootProject.file("app/google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "rs.tim13.slagalica"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "rs.tim13.slagalica"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiBaseUrl = localProperties.getProperty("API_BASE_URL") ?: "\"http://10.0.2.2:3000\""

        buildConfigField("String", "API_BASE_URL", apiBaseUrl)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Firebase Cloud Messaging (spec 11) — push notifikacije i kad app nije u prvom planu.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.4.0")
    // QR kod: generisanje (profil) i skeniranje (dodavanje prijatelja) — spec 2.a.viii i 7.b
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    // Mapa regiona preko OpenStreetMap-a (spec 5)
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.lottie)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}