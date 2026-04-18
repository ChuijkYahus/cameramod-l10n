plugins {
    id("com.possible-triangle.neoforge")
}

neoforge {
    dependOn(project(":common"))
}


dependencies {
    neoForge "net.neoforged:neoforge:${neo_version}"

    common(project(path: ':common', configuration: 'namedElements')) { transitive false }
    shadowCommon(project(path: ':common', configuration: 'transformProductionNeoForge'))

    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.0"))
    implementation(include("io.github.llamalad7:mixinextras-neoforge:0.5.0"))

    implementation 'org.jetbrains:annotations:22.0.0'

    modApi("net.mehvahdjukaar:moonlight-neoforge:${moonlight_version}"){
        transitive = true
    }

    modCompileOnly("curse.maven:exposure-871755:7033927")
    modImplementation("curse.maven:jei-238222:7057366")
    modCompileOnly("curse.maven:cc-tweaked-282001:5714512")
    //modImplementation("cc.tweaked-cobalt:cobalt:0.93")
    modCompileOnly("net.mehvahdjukaar:supplementaries-neoforge:${supplementaries_version}"){
        transitive = false
    }

  //  modImplementation("cc.tweaked:cc-tweaked-1.21.1-forge:1.117.0")

}
