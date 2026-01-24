import java.util.Base64

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    id("maven-publish")
    signing
}

//val KEY_PAGE_NAME = "pageName"

val namespace = "io.github.ailuoku6"

kotlin {

    /* ---------- Android（只提供 variant，不承载 Kuikly runtime） ---------- */
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        publishLibraryVariants("release")
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
//                compileOnly("com.tencent.kuikly-open:core-annotations:${Version.getKuiklyVersion()}")
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
    namespace = "io.github.ailuoku6.kisstate"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
}

/* ---------- Publishing ---------- */
group = "io.github.ailuoku6.kisstate"
version = System.getenv("kuiklyBizVersion") ?: "1.0.3"

//base {
//    archivesName.set("kisstate")
//}

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
            name = "ossrh-staging-api"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")

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

tasks.register<Exec>("uploadToCentralPortal") {
    group = "publishing"
    description = "Notify Central Portal that maven-publish upload is complete"

    val username = findProperty("sonatypeUsername") as String
    val password = findProperty("sonatypePassword") as String

    val auth = Base64.getEncoder()
        .encodeToString("$username:$password".toByteArray())

    commandLine(
        "curl",
        "-X", "POST",
        "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/$namespace",
        "-H", "Authorization: Bearer $auth",
        "-H", "Content-Type: application/json"
    )
}

tasks.named("publish") {
    finalizedBy("uploadToCentralPortal")
}

tasks.register<Exec>("dropCentralStagingRepositories") {
    group = "publishing"
    description = "Drop all OSSRH staging repositories for current IP"

    val username = findProperty("sonatypeUsername") as String
    val password = findProperty("sonatypePassword") as String

    val auth = Base64.getEncoder()
        .encodeToString("$username:$password".toByteArray())

    commandLine(
        "bash", "-c", """
        set -e
        
        REPOS=${'$'}(curl -s \
          "https://ossrh-staging-api.central.sonatype.com/manual/search/repositories?profile_id=$namespace&ip=client" \
          -H "Authorization: Bearer $auth" \
          | jq -r '.repositories[].key')
        
        for repo in ${'$'}REPOS; do
          echo "Dropping repository: ${'$'}repo"
          curl -X DELETE \
            "https://ossrh-staging-api.central.sonatype.com/manual/drop/repository/${'$'}repo" \
            -H "Authorization: Bearer $auth"
        done
        """.trimIndent()
    )
}

tasks.register<Exec>("searchCentralStagingRepositories") {
    group = "publishing"
    description = "Search OSSRH staging repositories for current IP"

    val username = findProperty("sonatypeUsername") as String
    val password = findProperty("sonatypePassword") as String

    val auth = Base64.getEncoder()
        .encodeToString("$username:$password".toByteArray())

    commandLine(
        "curl",
        "-s",
        "-X", "GET",
        "https://ossrh-staging-api.central.sonatype.com/manual/search/repositories" +
                "?profile_id=$namespace&ip=client",
        "-H", "Authorization: Bearer $auth",
        "-H", "Accept: application/json"
    )
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

