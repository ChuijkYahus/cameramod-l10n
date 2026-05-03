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

    modImplementation("curse.maven:supplementaries-412082:7892406")

    //modImplementation("cc.tweaked-cobalt:cobalt:0.93")
    modCompileOnly("curse.maven:exposure-871755:7033927")
    modCompileOnly("curse.maven:cc-tweaked-282001:5714512")
    modCompileOnly("curse.maven:jei-238222:7420587")
    modCompileOnly("net.mehvahdjukaar:supplementaries-neoforge:${supplementaries_version}")
    modCompileOnly("curse.maven:sodium-394468:6382649")
    modCompileOnly("curse.maven:irisshaders-455508:6213635")
    modCompileOnly("curse.maven:distant-horizons-508933:7977110")


    //  modImplementation("cc.tweaked:cc-tweaked-1.21.1-forge:1.117.0")
}
