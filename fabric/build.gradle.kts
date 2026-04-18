plugins {
    id("com.possible-triangle.fabric")
}

fabric {
    dependOn(project(":common"))
}

// This is ony way to do it in kotlin
// the other is in neoforge/build.gradle
// the third would also be version catalogs
val moonlight_version: String by extra
val supplementaries_version: String by extra

dependencies {

    modApi("net.mehvahdjukaar:moonlight-fabric:${moonlight_version}") {
        isTransitive = false
    }

    modRuntimeOnly("net.mehvahdjukaar:supplementaries-fabric:${supplementaries_version}") {
        isTransitive = false
    }

    modImplementation("curse.maven:sodium-394468:6382649")
    modCompileOnly("curse.maven:irisshaders-455508:6213635")
    modCompileOnly("curse.maven:distant-horizons-508933:7336780")
    modCompileOnly("curse.maven:cc-tweaked-282001:5714511")
    modCompileOnly("curse.maven:modmenu-308702:3920481")
    // modImplementation("cc.tweaked-cobalt:cobalt:0.93")
    // modCompileOnly "com.terraformersmc:modmenu:4.0.6",
}