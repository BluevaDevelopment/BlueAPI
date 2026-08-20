import java.util.Properties

val buildProps = Properties().apply {
    file("build.properties").inputStream().use { load(it) }
}
val revision = "${buildProps["year"]}.${buildProps["build"]}"

allprojects {
    group = "net.blueva.foundation"
    version = revision

    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") { name = "spigot-repo" }
        maven("https://oss.sonatype.org/content/repositories/snapshots/") { name = "sonatype" }
        maven("https://libraries.minecraft.net/") { name = "mojang" }
        maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
        maven("https://maven.hytale.com/release") { name = "hytale-release" }
        maven("https://maven.hytale.com/pre-release") { name = "hytale-pre-release" }
        maven("https://www.cursemaven.com") { name = "cursemaven" }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }

    tasks.withType<Test> {
        useJUnit()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    // Bakes BuildInfo.VERSION in as a compile-time constant (see src/main/java-templates),
    // so BlueFoundation.version() stays accurate once shaded into a consumer jar - the
    // manifest and the Maven/Gradle module metadata describe that jar instead, or are
    // stripped from it entirely.
    val templatesDir = file("src/main/java-templates")
    if (templatesDir.exists()) {
        val generateBuildInfo = tasks.register<Copy>("generateBuildInfo") {
            from(templatesDir)
            into(layout.buildDirectory.dir("generated/sources/buildInfo/java"))
            expand("version" to project.version)
        }
        extensions.configure<JavaPluginExtension> {
            sourceSets.named("main") {
                java.srcDir(generateBuildInfo.map { it.destinationDir })
            }
        }
        tasks.named("compileJava") { dependsOn(generateBuildInfo) }
    }

    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "blueva-repo"
                url = uri("https://repo.blueva.net/releases")
                credentials {
                    username = System.getenv("BLUEVA_REPO_USERNAME")
                    password = System.getenv("BLUEVA_REPO_SECRET")
                }
            }
        }
    }
}
