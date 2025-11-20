pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "BeatU-Client"
include(":app")

// Core modules
include(":core:common")
include(":core:network")
include(":core:database")
include(":core:player")
include(":core:designsystem")

// Domain module
include(":domain")

// Data module
include(":data")

// Feature modules
include(":feature:feed")
include(":feature:landscape")
include(":feature:profile")
include(":feature:search")
include(":feature:settings")
include(":feature:aiassistant")
