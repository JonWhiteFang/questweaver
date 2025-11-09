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
include(":sync:firebase")
