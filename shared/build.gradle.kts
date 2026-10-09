plugins {
    java
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

setupCommonUnmodded("shared", javaVersion = 17)

dependencies {
    api(project(":api"))
    api(libs.bundles.kotlin)
    api(libs.datafixerupper.get())
    api(libs.modernnetworking.api)
    compileOnly(libs.netty)
}
