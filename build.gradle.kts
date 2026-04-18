plugins {
    id("com.possible-triangle.core")
    id("com.possible-triangle.common") apply false
    id("com.possible-triangle.fabric") apply false
    id("com.possible-triangle.forge") apply false
    id("com.possible-triangle.neoforge") apply false
}

import java.io.ByteArrayOutputStream

subprojects {
    apply(plugin = "com.possible-triangle.core")

    upload {
        maven {
            nexus()
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add(
            "-Acrafttweaker.processor.document.output_directory=${
                File(rootProject.buildDir, "docsOut")
            }"
        )
        options.compilerArgs.add(
            "-Acrafttweaker.processor.document.multi_source=true"
        )

        options.compilerArgs.addAll(listOf("-Xmaxerrs", "4000"))
    }
}

tasks.register("buildAndPublishAll") {
    dependsOn(":clean")
    dependsOn(":common:clean")
    dependsOn(":fabric:clean")
    dependsOn(":neoforge:clean")
    dependsOn(":build")
    dependsOn(":neoforge:build")
    dependsOn(":fabric:build")
    dependsOn(":neoforge:curseforge")
    dependsOn(":neoforge:modrinth")
    dependsOn(":fabric:curseforge")
    dependsOn(":fabric:modrinth")
    dependsOn(":publish")
    dependsOn(":fabric:publish")
    dependsOn(":neoforge:publish")
    dependsOn(":publishToMavenLocal")

    finalizedBy(":gitTag")

    group = "build"
    description = "Runs clean, build, and publish neoforge and fabric"
}

tasks.register("gitTag") {
    group = "build"
    description = "Create and push git tag from project version"

    doLast {
        val tag = project.version.toString()

        // check if tag already exists
        val stdout = ByteArrayOutputStream()

        exec {
            commandLine("git", "tag", "-l", tag)
            standardOutput = stdout
        }

        if (stdout.toString().trim().isNotEmpty()) {
            logger.warn("Git tag '$tag' already exists")
        } else {

            // create annotated tag
            exec {
                commandLine("git", "tag", "-a", tag, "-m", "Release $tag")
            }

            // push tag
            exec {
                commandLine("git", "push", "origin", tag)
            }
        }
    }
}