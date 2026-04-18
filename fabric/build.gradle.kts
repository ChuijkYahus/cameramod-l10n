plugins {
    id("com.possible-triangle.fabric")
}

fabric {
    dependOn(project(":common"))
}


dependencies {
    modImplementation "net.fabricmc:fabric-loader:${fabric_loader_version}"
    modApi "net.fabricmc.fabric-api:fabric-api:${fabric_api_version}"


    common(project(path: ":common", configuration: "namedElements")) { transitive false }
    shadowCommon(project(path: ":common", configuration: "transformProductionFabric")) { transitive false }

    include(implementation(annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.5.0")))

    modApi("net.mehvahdjukaar:moonlight-fabric:${moonlight_version}"){
        transitive = false
    }

    modRuntimeOnly("net.mehvahdjukaar:supplementaries-fabric:${supplementaries_version}"){
        transitive = false
    }


    modImplementation("curse.maven:sodium-394468:6382649")
    modCompileOnly("curse.maven:irisshaders-455508:6213635")
    modCompileOnly("curse.maven:distant-horizons-508933:7336780")
    modCompileOnly("curse.maven:cc-tweaked-282001:5714511")
    modCompileOnly("curse.maven:modmenu-308702:3920481")
    //modImplementation("cc.tweaked-cobalt:cobalt:0.93")
   // modCompileOnly "com.terraformersmc:modmenu:4.0.6",
}
