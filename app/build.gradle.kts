
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.legacy.kapt)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.readmd.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.readmd.app"
        minSdk = 26
        targetSdk = 36
        // Release builds get both from the git tag (see scripts/version-from-tag.sh and the
        // release workflow), passed as -PversionName and -PversionCode. Local builds are "dev".
        versionCode = providers.gradleProperty("versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("versionName").orNull ?: "dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // The real signing key comes from release.keystore and three environment variables (the
    // release workflow provides them from repository secrets). Without them, for example in a
    // fork or on a developer machine, release builds fall back to the debug key so they still
    // produce an installable APK. The release workflow sets RELEASE_SIGNING_REQUIRED, which
    // turns a missing secret into a build failure instead of a debug-signed release.
    val releaseKeystore = rootProject.file("release.keystore")
    val releaseSigningAvailable = releaseKeystore.exists() &&
        listOf("ANDROID_KEYSTORE_PASSWORD", "ANDROID_KEY_ALIAS", "ANDROID_KEY_PASSWORD")
            .all { !System.getenv(it).isNullOrEmpty() }
    if (System.getenv("RELEASE_SIGNING_REQUIRED") == "true" && !releaseSigningAvailable) {
        throw GradleException(
            "Release signing is required but release.keystore or one of ANDROID_KEYSTORE_PASSWORD, " +
                "ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD is missing.",
        )
    }

    signingConfigs {
        if (releaseSigningAvailable) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName(if (releaseSigningAvailable) "release" else "debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// prism4j pulls org.jetbrains:annotations-java5, whose classes duplicate
// org.jetbrains:annotations (from the Kotlin stdlib). Both are compile-time
// annotations only, so drop the java5 copy to fix checkDebugDuplicateClasses.
configurations.configureEach {
    exclude(group = "org.jetbrains", module = "annotations-java5")
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.browser)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.markwon.core)
    implementation(libs.markwon.image)
    implementation(libs.markwon.ext.tables)
    implementation(libs.markwon.ext.strikethrough)
    implementation(libs.markwon.ext.tasklist)
    implementation(libs.markwon.ext.latex)
    implementation(libs.markwon.inline.parser)
    implementation(libs.markwon.syntax.highlight)
    implementation(libs.prism4j)
    kapt(libs.prism4j.bundler)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
