import com.tencent.kuikly.gradle.config.KuiklyConfig
import java.util.Base64

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("maven-publish")
//    id("com.tencent.kuikly-open.kuikly")
    signing
}

val KEY_PAGE_NAME = "pageName"
val namespace = "io.github.ailuoku6"

/* ---------- Publishing ---------- */
group = "io.github.ailuoku6.kisstate"
version = System.getenv("kuiklyBizVersion") ?: "1.0.7"

fun getPageName(): String {
    return (project.properties[KEY_PAGE_NAME] as? String) ?: ""
}

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
//        browser()
        browser {
            webpackTask {
                outputFileName = "nativevue2.js" // 最后输出的名字
            }

            commonWebpackConfig {
                output?.library = null // 不导出全局对象，只导出必要的入口函数
                devtool = "source-map" // 不使用默认的 eval 执行方式构建出 source-map，而是构建单独的 sourceMap 文件
            }
        }
        binaries.executable() //将kotlin.js与kotlin代码打包成一份可直接运行的js文件
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
            freeCompilerArgs = freeCompilerArgs + getCommonCompilerArgs()
            license = "MIT"
        }
        extraSpecAttributes["resources"] = "['src/commonMain/assets/**']"
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

ksp {
    arg("enableMultiModule","true")
    arg("isMainModule", "false")
}

dependencies {
    compileOnly("com.tencent.kuikly-open:core-ksp:${Version.getKuiklyVersion()}") {
        add("kspAndroid", this)
        add("kspIosArm64", this)
        add("kspIosX64", this)
        add("kspIosSimulatorArm64", this)
        add("kspJs", this)
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

// Kuikly 插件配置
//configure<KuiklyConfig> {
//    // JS 产物配置
//    js {
//        // 构建产物名，与 KMM 插件 webpackTask#outputFileName 一致
//        outputName("nativevue2")
//        // 可选：分包构建时的页面列表，如果为空则构建全部页面
//        // addSplitPage("route","home")
//    }
//}

fun getCommonCompilerArgs(): List<String> {
    return listOf(
        "-Xallocator=std"
    )
}

fun getLinkerArgs(): List<String> {
    return listOf()
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

