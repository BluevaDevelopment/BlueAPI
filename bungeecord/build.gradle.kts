// BungeeCord has no Adventure of its own - text goes through its own
// BaseComponent/TextComponent chat API (net.md-5:bungeecord-chat, pulled in
// transitively by bungeecord-api). We bridge to it via BlueFoundation's own
// legacy-section serializer rather than shipping a second component tree.

base {
    archivesName.set("BlueFoundation-BungeeCord")
}

tasks.withType<JavaCompile> {
    options.release.set(17)
}

dependencies {
    api(project(":common"))

    compileOnly(libs.bungeecord.api)

    testImplementation(libs.junit)
    testImplementation(libs.bungeecord.api)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "BlueFoundation-BungeeCord"
        }
    }
}
