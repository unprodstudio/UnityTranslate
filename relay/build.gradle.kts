plugins {
    java
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

setupCommonUnmodded("relay", javaVersion = 25)

dependencies {
    api(project(":shared"))
    api(libs.bundles.kotlin)
    api(libs.datafixerupper.get())
    api(libs.netty)
    api(libs.modernnetworking.api)
    api(libs.sunset)
}
