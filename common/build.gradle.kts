import org.gradle.kotlin.dsl.accessTransformers

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

    modCompileOnly("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")
    accessTransformers("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}")


    //modImplementation("net.mehvahdjukaar:supplementaries-neoforge:${supplementaries_version}")
    modImplementation("curse.maven:supplementaries-412082:8044262")


    modCompileOnly("curse.maven:exposure-871755:7862664")
    modCompileOnly("curse.maven:distant-horizons-508933:7977110")
    modCompileOnly("maven.modrinth:iris:1.8.8+1.21.1-neoforge")

    modCompileOnly("curse.maven:cc-tweaked-282001:5714512")
    //modCompileOnly files("mods/entityculling-fabric-1.10.1-mc1.21.1.jar")
    //modCompileOnly("maven.modrinth:entityculling:1.10.1-1.21.1+-+NeoForge")
    //modCompileOnly "curse.maven:entityculling-448233:7900085"
    modCompileOnly("curse.maven:jei-238222:7420587")
    //modCompileOnly("curse.maven:irisshaders-455508:6213635")
    modCompileOnly("curse.maven:sodium-394468:6382651")

    modCompileOnly("curse.maven:alexs-caves-924854:4806837")
    modImplementation("curse.maven:watermedia-869524:7072353")
}
