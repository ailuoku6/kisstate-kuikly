plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    id("maven-publish")
//    signing
}

val KEY_PAGE_NAME = "pageName"

kotlin {

    /* ---------- Android（只提供 variant，不承载 Kuikly runtime） ---------- */
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    /* ---------- iOS ---------- */
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    /* ---------- JS ---------- */
    js(IR) {
        browser()
    }

    cocoapods {
        summary = "kisstate Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "14.1"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "kisstate"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                compileOnly("com.tencent.kuikly-open:core:${Version.getKuiklyVersion()}")
                compileOnly("com.tencent.kuikly-open:core-annotations:${Version.getKuiklyVersion()}")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {

            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

/* ---------- Android 配置 ---------- */
android {
    namespace = "com.ailuoku6.kisstate.core"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
}

/* ---------- Publishing ---------- */
group = "com.ailuoku6.kisstate"
version = System.getenv("kuiklyBizVersion") ?: "1.0.0"

publishing {
    repositories {
        maven {
            credentials {
                username = System.getenv("mavenUserName") ?: ""
                password = System.getenv("mavenPassword") ?: ""
            }
            rootProject.properties["mavenUrl"]?.toString()?.let {
                url = uri(it)
            }
        }
    }
}

/* ---------- Utils ---------- */
fun getPageName(): String {
    return (project.properties[KEY_PAGE_NAME] as? String) ?: ""
}
