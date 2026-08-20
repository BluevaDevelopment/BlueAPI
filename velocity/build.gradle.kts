// Velocity is Adventure-native end-to-end (ProxyServer/Player/CommandSource
// all implement Audience), so this module needs none of the Bukkit legacy
// reflection tricks - it leans directly on net.kyori.adventure and Velocity's
// own APIs. Velocity 4.0 requires JVM 25.

base {
    archivesName.set("BlueFoundation-Velocity")
}

tasks.withType<JavaCompile> {
    options.release.set(25)
}

dependencies {
    api(project(":common"))

    compileOnly(libs.velocity.api)

    testImplementation(libs.junit)
    testImplementation(libs.velocity.api)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "BlueFoundation-Velocity"
        }
    }
}
