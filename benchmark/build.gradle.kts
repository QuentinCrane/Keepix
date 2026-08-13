plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.futureape.kanleme.benchmark"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.benchmark:benchmark-macro-junit4:1.4.1")
    implementation("androidx.test.ext:junit:1.2.1")
}

val baselineProfileUseConnectedDevices = providers.gradleProperty("kanleme.baselineProfile.useConnectedDevices")
    .map { it.toBoolean() }
    .getOrElse(false)

baselineProfile {
    useConnectedDevices = baselineProfileUseConnectedDevices
}
