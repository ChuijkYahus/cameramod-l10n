plugins {
    id("com.possible-triangle.neoforge")
}

neoforge {
    dependOn(project(":common"))
    accessWidener(project(":common"))
}

val moonlight_version: String by extra
val supplementaries_version: String by extra

dependencies {

    modImplementation("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")
    accessTransformers("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")

    modImplementation("net.mehvahdjukaar:supplementaries-neoforge:${supplementaries_version}")

    //modImplementation("cc.tweaked-cobalt:cobalt:0.93")
    modCompileOnly("curse.maven:exposure-871755:7862664")
    modCompileOnly("curse.maven:cc-tweaked-282001:5714512")
    modCompileOnly("curse.maven:jei-238222:7420587")
    modCompileOnly("curse.maven:sodium-394468:6382649")
    modCompileOnly("curse.maven:irisshaders-455508:6213635")
    modCompileOnly("curse.maven:distant-horizons-508933:7977110")
    modCompileOnly("maven.modrinth:iris:1.8.8+1.21.1-neoforge")

    modCompileOnly("curse.maven:watermedia-869524:7072353")

    //  modImplementation("cc.tweaked:cc-tweaked-1.21.1-forge:1.117.0")
}
