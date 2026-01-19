plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    id("maven-publish")
    signing
}

//val KEY_PAGE_NAME = "pageName"

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
    namespace = "io.github.ailuoku6"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
}

/* ---------- Publishing ---------- */
group = "io.github.ailuoku6"
version = System.getenv("kuiklyBizVersion") ?: "1.0.0"

publishing {
    publications {
        withType<MavenPublication>().configureEach {
            pom {
                name.set("kisstate")
                description.set("A lightweight reactive state management library for Kuikly")
                url.set("https://github.com/ailuoku6/kisstate-kuikly")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("ailuoku6")
                        name.set("ailuoku6")
                        email.set("ailuoku6@qq.com")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/ailuoku6/kisstate-kuikly.git")
                    developerConnection.set("scm:git:ssh://git@github.com:ailuoku6/kisstate.git")
                    url.set("https://github.com/ailuoku6/kisstate-kuikly")
                }
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            url = uri(
                if (version.toString().endsWith("SNAPSHOT"))
                    "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                else
                    "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            )

            credentials {
                username = findProperty("sonatypeUsername") as String?
                password = findProperty("sonatypePassword") as String?
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

signing {
    val keyId = findProperty("signing.keyId") as String?
    val key = findProperty("signing.key") as String?
    val password = findProperty("signing.password") as String?

    check(keyId != null) { "signing.keyId not found" }
    check(key != null) { "signing.key not found" }
    check(password != null) { "signing.password not found" }

    useInMemoryPgpKeys(keyId, key, password)
    sign(publishing.publications)
}



/* ---------- Utils ---------- */
//fun getPageName(): String {
//    return (project.properties[KEY_PAGE_NAME] as? String) ?: ""
//}
