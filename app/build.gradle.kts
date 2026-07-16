import java.util.Properties

plugins {
    alias(libs.plugins.android.application)

    // Giữ dòng kotlin android nếu project đang có sẵn
    // Ví dụ:
    // alias(libs.plugins.kotlin.android)

    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun String.toBuildConfigString(): String {
    return buildString(length + 2) {
        append('"')
        this@toBuildConfigString.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                else -> append(character)
            }
        }
        append('"')
    }
}

val paymentBackendUrl =
    localProperties.getProperty("PIZZATIME_PAYMENT_BACKEND_URL", "").trim()

android {
    namespace = "com.devpro.pizzatime"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.devpro.pizzatime"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["deliveryTrackingServiceEnabled"] = "false"
        buildConfigField(
            "String",
            "PAYMENT_BACKEND_URL",
            paymentBackendUrl.toBuildConfigString(),
        )
    }

    flavorDimensions += "role"

    productFlavors {
        create("guest") {
            dimension = "role"
            applicationIdSuffix = ".guest"
            versionNameSuffix = "-guest"

            buildConfigField(
                "String",
                "APP_EDITION",
                "\"GUEST\"",
            )

            resValue(
                "string",
                "app_name",
                "PizzaTime Guest",
            )
        }

        create("customer") {
            dimension = "role"
            versionNameSuffix = "-customer"

            buildConfigField(
                "String",
                "APP_EDITION",
                "\"CUSTOMER\"",
            )

            resValue(
                "string",
                "app_name",
                "PizzaTime",
            )
        }

        create("staff") {
            dimension = "role"
            applicationIdSuffix = ".staff"
            versionNameSuffix = "-staff"

            buildConfigField(
                "String",
                "APP_EDITION",
                "\"STAFF\"",
            )

            resValue(
                "string",
                "app_name",
                "PizzaTime Staff",
            )
        }

        create("kitchen") {
            dimension = "role"
            applicationIdSuffix = ".kitchen"
            versionNameSuffix = "-kitchen"

            buildConfigField(
                "String",
                "APP_EDITION",
                "\"KITCHEN\"",
            )

            resValue(
                "string",
                "app_name",
                "PizzaTime Kitchen",
            )
        }

        create("shipper") {
            dimension = "role"
            applicationIdSuffix = ".shipper"
            versionNameSuffix = "-shipper"
            manifestPlaceholders["deliveryTrackingServiceEnabled"] = "true"

            buildConfigField(
                "String",
                "APP_EDITION",
                "\"SHIPPER\"",
            )

            resValue(
                "string",
                "app_name",
                "PizzaTime Shipper",
            )
        }

        create("admin") {
            dimension = "role"
            applicationIdSuffix = ".admin"
            versionNameSuffix = "-admin"

            buildConfigField(
                "String",
                "APP_EDITION",
                "\"ADMIN\"",
            )

            resValue(
                "string",
                "app_name",
                "PizzaTime Admin",
            )
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.material)
    implementation(libs.osmdroid.android)
    implementation(libs.zxing.core)
    implementation("androidx.lifecycle:lifecycle-process:2.8.4")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.16")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
