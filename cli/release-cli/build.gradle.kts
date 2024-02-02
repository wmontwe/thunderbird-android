plugins {
    id(ThunderbirdPlugins.App.jvm)
}

version = "unspecified"

application {
    mainClass.set("net.thunderbird.cli.release.MainKt")
}

dependencies {
    implementation(libs.clikt)
}
