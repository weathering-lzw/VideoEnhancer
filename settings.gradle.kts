pluginManagement {
    repositories {
        // 使用腾讯云镜像代理 Google Maven
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/google/") }
        // 备用镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // Maven Central 镜像
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Google Maven 镜像
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/google/") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // Maven Central 镜像
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        mavenCentral()
        // JitPack
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "VideoEnhancer"
include(":app")
