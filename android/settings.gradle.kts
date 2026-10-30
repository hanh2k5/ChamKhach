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

// ---------------------------------------------------------------------------
// Tự động xử lý local.properties tránh xung đột giữa WSL2 và Windows
// ---------------------------------------------------------------------------
val isLinux = !System.getProperty("os.name").lowercase().contains("windows")
val localPropsFile = file("local.properties")
val winPropsFile = file("local.properties.win")

if (isLinux) {
    val linuxSdk = System.getenv("ANDROID_HOME") ?: "${System.getProperty("user.home")}/android-sdk"
    if (localPropsFile.exists()) {
        val content = localPropsFile.readText()
        if (content.contains(":\\") || content.contains(":/")) {
            // Đang chứa đường dẫn Windows từ Android Studio -> lưu sang local.properties.win
            winPropsFile.writeText(content)
            localPropsFile.writeText("sdk.dir=$linuxSdk\n")
        }
    } else {
        localPropsFile.writeText("sdk.dir=$linuxSdk\n")
    }
} else {
    // Đang chạy trên Windows
    if (localPropsFile.exists()) {
        val content = localPropsFile.readText()
        if (content.contains("/home/") && winPropsFile.exists()) {
            localPropsFile.writeText(winPropsFile.readText())
        }
    }
}

rootProject.name = "TelesalesAssistant"
include(":app")

