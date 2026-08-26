plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.mapgie.goflo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mapgie.goflo"
        minSdk = 26
        targetSdk = 34
        versionCode = 119
        versionName = "0.55.0-beta.1"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("../debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        buildConfig = true
    }

    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = true
        warningsAsErrors = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.work.runtime.ktx)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
    // Real org.json for JVM unit tests — the android.jar stubs throw
    // "not mocked" for JSONObject/JSONArray, which the export/import
    // round-trip tests exercise. Deliberately not in libs.versions.toml:
    // test-only dependencies are never shipped, so they don't belong on
    // the licenses attribution screen the catalog check keeps in sync.
    testImplementation("org.json:json:20240303")
    // Real SQLite engine for JVM unit tests — lets migration tests execute the
    // actual Migration SQL against a real database (Room's MigrationTestHelper
    // needs instrumented tests + exported schemas, neither of which this
    // project has). Test-only, so deliberately not in libs.versions.toml
    // (same rationale as org.json above).
    testImplementation("org.xerial:sqlite-jdbc:3.45.1.0")
}

// ── Keep assets/CHANGELOG.md in sync with the root copy ──────────────────────
// Runs before every build so the "What's New" dialog always reflects the latest
// entries without requiring a manual copy step.
tasks.register<Copy>("syncChangelog") {
    description = "Copies root CHANGELOG.md into app/src/main/assets before each build."
    from(rootProject.file("CHANGELOG.md"))
    into("src/main/assets")
}

tasks.named("preBuild") {
    dependsOn("syncChangelog")
}
