// The original BlueFoundation module. Artifact coordinates are unchanged
// (net.blueva.foundation:BlueFoundation) so every existing consumer's
// dependency declaration keeps working untouched.

base {
    archivesName.set("BlueFoundation")
}

tasks.withType<JavaCompile> {
    options.release.set(8)
}

dependencies {
    api(project(":common"))

    compileOnly(libs.spigot.api) {
        exclude(group = "net.md-5", module = "bungeecord-chat")
    }
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.minimessage)
    compileOnly(libs.adventure.serializer.legacy)
    compileOnly(libs.authlib)
    compileOnly(libs.netty.all)
    compileOnly(libs.gson)

    testImplementation(libs.junit)
    testImplementation(libs.spigot.api) {
        exclude(group = "net.md-5", module = "bungeecord-chat")
    }
    testImplementation(libs.adventure.api)
    testImplementation(libs.adventure.minimessage)
    testImplementation(libs.adventure.serializer.legacy)
    testImplementation(libs.gson)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "BlueFoundation"
        }
    }
}
