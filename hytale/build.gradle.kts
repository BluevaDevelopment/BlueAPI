// Hytale has neither Adventure nor a Bukkit-style scheduler/config system of
// its own - text goes through its own Message builder, and config/dependency
// loading reuse :common's engine standalone rather than Hytale's Codec/
// BuilderCodec system. Server-side scheduling piggybacks on
// HytaleServer.SCHEDULED_EXECUTOR, since TaskRegistry only tracks futures for
// cleanup and has no runTaskLater-style API of its own.

base {
    archivesName.set("BlueFoundation-Hytale")
}

tasks.withType<JavaCompile> {
    options.release.set(21)
}

dependencies {
    api(project(":common"))

    compileOnly(libs.hytale.server)

    // Compile-time only: net.blueva.foundation.scoreboard.HyUiBridge is
    // written against this normally (no reflection), but the jar is never
    // shaded in - Scoreboards downloads it into the plugin's own classloader
    // at runtime instead (see Scoreboards#ensureLibraryLoaded). curse.maven
    // coordinates pin a specific CurseForge file id (there is no "latest"
    // alias); bump the id here (and in Scoreboards) when updating HyUI.
    // https://www.curseforge.com/hytale/mods/hyui/files
    compileOnly("curse.maven:hyui-1431415:8363317")

    testImplementation(libs.junit)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "BlueFoundation-Hytale"
        }
    }
}
