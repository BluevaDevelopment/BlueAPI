// Platform-agnostic core shared by every BlueFoundation module: the runtime
// dependency downloader, the config engine, and the MiniMessage/component
// text stack. No Bukkit/Velocity/BungeeCord/Hytale API on the compile
// classpath - if a class here needs one, it belongs in a platform module.

base {
    archivesName.set("BlueFoundation-Common")
}

tasks.withType<JavaCompile> {
    options.release.set(8)
}

dependencies {
    compileOnly(libs.gson)
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.minimessage)
    compileOnly(libs.adventure.serializer.legacy)

    testImplementation(libs.junit)
    testImplementation(libs.gson)
    testImplementation(libs.adventure.api)
    testImplementation(libs.adventure.minimessage)
    testImplementation(libs.adventure.serializer.legacy)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "BlueFoundation-Common"
        }
    }
}
