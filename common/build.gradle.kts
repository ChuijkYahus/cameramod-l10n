plugins {
    id("com.possible-triangle.common")
}

common {
    accessWidener()
}

val moonlight_version: String by extra
val supplementaries_version: String by extra
val candlelight_version: String by extra

dependencies {
    compileOnly("net.mehvahdjukaar:candlelight:${candlelight_version}")

    modImplementation("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")

  //  modCompileOnly("net.mehvahdjukaar:supplementaries:${supplementaries_version}")
    // modImplementation("curse.maven:selene-499980:7113116")

    modImplementation("curse.maven:exposure-871755:7033927")
    modCompileOnly("curse.maven:distant-horizons-508933:7336780")

    modCompileOnly("curse.maven:cc-tweaked-282001:5714512")
    //modCompileOnly files("mods/entityculling-fabric-1.10.1-mc1.21.1.jar")
    //modCompileOnly("maven.modrinth:entityculling:1.10.1-1.21.1+-+NeoForge")
    //modCompileOnly "curse.maven:entityculling-448233:7900085"
    modCompileOnly("curse.maven:jei-238222:5846878")
    modCompileOnly("curse.maven:irisshaders-455508:6213635")
    modCompileOnly("curse.maven:sodium-394468:6382649")

    modCompileOnly("curse.maven:alexs-caves-924854:4806837")
}
