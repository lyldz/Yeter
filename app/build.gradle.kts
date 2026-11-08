plugins {
    id("com.android.application") version "8.3.0"
    kotlin("android") version "1.9.23"
}

android {
    namespace = "com.yeter.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yeter.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 9
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Sabit imza: her derleme aynı anahtarla imzalanır, böylece sürümler birbirinin üstüne kurulabilir.
    // Anahtar (app/yeter.jks) CI'da gizli değerlerden oluşturulur; yoksa varsayılan debug imzası kullanılır.
    val yeterKeystore = file("yeter.jks")
    signingConfigs {
        if (yeterKeystore.exists()) {
            create("yeter") {
                storeFile = yeterKeystore
                storePassword = System.getenv("YETER_KS_PASS")
                keyAlias = "yeter"
                keyPassword = System.getenv("YETER_KS_PASS")
            }
        }
    }
    buildTypes {
        getByName("debug") {
            if (yeterKeystore.exists()) signingConfig = signingConfigs.getByName("yeter")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
