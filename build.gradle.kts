import groovy.json.JsonSlurper
import java.util.zip.ZipFile

plugins {
    id("fabric-loom") version "1.15.5"
}

version = property("mod_version") as String
group = property("maven_group") as String
base { archivesName.set(property("archives_base_name") as String) }

val cobblemonJar = providers.gradleProperty("cobblemonJar").orNull?.let(::file)
    ?: throw GradleException("Fournir un JAR Cobblemon Fabric avec -PcobblemonJar=<fichier>.")
check(cobblemonJar.isFile) { "Le JAR Cobblemon fourni est introuvable." }
val cobblemonMinimumVersion = property("cobblemon_min_version") as String

fun versionParts(version: String): List<Int> {
    val parts = version.substringBefore('+').substringBefore('-').split('.')
    check(parts.size == 3 && parts.all { it.toIntOrNull() != null }) {
        "Version Cobblemon non reconnue : $version"
    }
    return parts.map(String::toInt)
}

fun isAtLeast(actual: List<Int>, minimum: List<Int>): Boolean {
    for (index in minimum.indices) {
        if (actual[index] != minimum[index]) return actual[index] > minimum[index]
    }
    return true
}

val verifyCobblemonCompatibility = tasks.register("verifyCobblemonCompatibility") {
    inputs.file(cobblemonJar)
    inputs.file("src/main/resources/fabric.mod.json")
    inputs.property("cobblemonMinimumVersion", cobblemonMinimumVersion)
    doLast {
        check(isAtLeast(versionParts(cobblemonMinimumVersion), listOf(1, 8, 0))) {
            "Le minimum pris en charge ne peut pas être inférieur à Cobblemon 1.8.0."
        }
        val dependency = "\"cobblemon\": \">=$cobblemonMinimumVersion\""
        check(file("src/main/resources/fabric.mod.json").readText().contains(dependency)) {
            "La dépendance fabric.mod.json doit correspondre au minimum déclaré."
        }
        val metadata = ZipFile(cobblemonJar).use { archive ->
            val entry = archive.getEntry("fabric.mod.json") ?: error("Métadonnées Fabric absentes.")
            archive.getInputStream(entry).reader().use { JsonSlurper().parse(it) as Map<*, *> }
        }
        check(metadata["id"] == "cobblemon") { "La dépendance fournie n'est pas Cobblemon." }
        val actualVersion = metadata["version"] as? String ?: error("Version Cobblemon absente.")
        check(isAtLeast(versionParts(actualVersion), versionParts(cobblemonMinimumVersion))) {
            "Cobblemon $actualVersion est antérieur au minimum $cobblemonMinimumVersion."
        }
        logger.lifecycle("JAR Cobblemon fourni vérifié : {}.", actualVersion)
    }
}
repositories { mavenCentral() }

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    modImplementation(files(cobblemonJar))
    modRuntimeOnly("net.fabricmc:fabric-language-kotlin:1.13.7+kotlin.2.2.21")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.ow2.asm:asm-tree:9.8")
}

java { withSourcesJar() }
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.compileJava { dependsOn(verifyCobblemonCompatibility) }
tasks.processResources {
    dependsOn(verifyCobblemonCompatibility)
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") { expand("version" to project.version) }
}

tasks.test {
    useJUnitPlatform()
    dependsOn(tasks.remapJar)
    inputs.file(cobblemonJar)
    inputs.file(tasks.remapJar.flatMap { it.archiveFile })
    doFirst {
        systemProperty("catchpreview.finalJar", tasks.remapJar.get().archiveFile.get().asFile.absolutePath)
        systemProperty("catchpreview.cobblemonJar", cobblemonJar.absolutePath)
    }
    providers.gradleProperty("coexistenceAuditDir").orNull?.let {
        systemProperty("catchpreview.coexistenceMods", it)
    }
}

val shareSources by tasks.registering(Zip::class) {
    archiveFileName.set("TropimonCatchPreview-${project.version}-sources.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from(projectDir) {
        include("src/main/java/**/*.java", "src/main/resources/fabric.mod.json",
            "src/main/resources/tropimon_catch_preview.mixins.json",
            "src/main/resources/assets/tropimon_catch_preview/lang/*.json",
            "src/main/resources/assets/tropimon_catch_preview/textures/gui/frame.png",
            "src/test/java/**/*.java",
            "gradlew", "gradlew.bat", "gradle/wrapper/gradle-wrapper.jar",
            "gradle/wrapper/gradle-wrapper.properties", "build.gradle.kts",
            "settings.gradle.kts", "gradle.properties", ".gitignore", "README.md",
            "LICENSE", "THIRD_PARTY_NOTICES.md", "licenses/GRADLE-WRAPPER-LICENSE.txt")
    }
}
