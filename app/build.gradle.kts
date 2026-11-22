import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.qamotech.gemstorm"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.qamotech.gemstorm"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "2.6.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
}

// GEMSTORM: audio ships in the repo (qamotech/gs) as base64 text because
// GitHub snapshot pushes are UTF-8 only. Decode app/b64assets/raw/*.wav.b64
// into src/main/res/raw before every build. Skips files already up to date.
val b64RawDir = file("b64assets/raw")
val outRawDir = file("src/main/res/raw")
val decodeB64Assets = tasks.register("decodeB64Assets") {
    inputs.dir(b64RawDir)
    outputs.dir(outRawDir)
    doLast {
        if (b64RawDir.isDirectory) {
            b64RawDir.listFiles { f -> f.name.endsWith(".wav.b64") }?.forEach { f ->
                val target = File(outRawDir, f.name.removeSuffix(".b64"))
                val bytes = Base64.getMimeDecoder().decode(f.readText())
                if (!target.exists() || target.length() != bytes.size.toLong()) {
                    target.writeBytes(bytes)
                }
            }
        }
    }
}
tasks.named("preBuild") { dependsOn(decodeB64Assets) }
