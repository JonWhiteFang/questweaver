pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // SQLCipher repository
        maven { url = uri("https://repo1.maven.org/maven2/") }
    }
}

rootProject.name = "QuestWeaver"

include(":app")
include(":core:domain")
include(":core:data")
include(":core:rules")
include(":feature:map")
include(":feature:encounter")
include(":feature:character")
include(":ai:ondevice")
include(":ai:gateway")
include(":ai:tactical")
include(":sync:firebase")
