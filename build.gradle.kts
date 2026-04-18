import java.io.ByteArrayOutputStream

plugins {
    id("com.possible-triangle.core")
    id("com.possible-triangle.common") apply false
    id("com.possible-triangle.fabric") apply false
    id("com.possible-triangle.neoforge") apply false
   // id("net.mehvahdjukaar.candlelight") version "1.0.0" apply false
}

mod {
    val mod_description: String by extra
    val mod_credits: String by extra
    val mod_license: String by extra
    val mod_homepage: String by extra
    val mod_github: String by extra
    val mod_authors: String by extra
    val moonlight_min_version: String by extra
    additional.add("mod_description", provider { mod_description })
    additional.add("mod_credits", provider { mod_credits })
    additional.add("mod_license", provider { mod_license })
    additional.add("mod_homepage", provider { mod_homepage })
    additional.add("mod_authors", provider { mod_homepage })
    additional.add("mod_github", provider { mod_github })
    additional.add("moonlight_min_version", provider { moonlight_min_version })
}


subprojects {

    apply(plugin = "com.possible-triangle.core")
    //apply(plugin = "net.mehvahdjukaar.candlelight")

    repositories {
        nexus()
    }

    upload {
        maven {
            nexus()
        }
    }
/*

    candlelight {
        clientOnly = false
        logging = true
    }
 */


    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf("-Xmaxerrs", "4000"))
    }
}

tasks.register("buildAndPublishAll") {
    // clean also calls clean for all subprojects, no need to reference them all specifically
    dependsOn("clean")
    // same for build
    dependsOn("build")
    // publish currently also already handles modrinth & curseforge if they are configured
    // might change that if you don't like it
    dependsOn("publish")


    finalizedBy("gitTag")

    group = "build"
    description = "Runs clean, build, and publish neoforge and fabric"
}

tasks.register("gitTag") {
    val execOps = project.objects.newInstance<ExecOperations>()

    group = "build"
    description = "Create and push git tag from project version"

    doLast {
        val tag = mod.version.get()

        // check if tag already exists
        val stdout = ByteArrayOutputStream()

        execOps.exec {
            commandLine("git", "tag", "-l", tag)
            standardOutput = stdout
        }

        if (!stdout.toString().trim().isEmpty()) {
            logger.warn("Git tag '${tag}' already exists")
        } else {
            // create annotated tag
            execOps.exec {
                commandLine("git", "tag", "-a", tag, "-m", "Release ${tag}")
            }

            // push tag
            execOps.exec {
                commandLine("git", "push", "origin", tag)
            }
        }
    }
}
