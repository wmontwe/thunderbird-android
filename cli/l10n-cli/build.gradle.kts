plugins {
    id(ThunderbirdPlugins.App.jvm)
}

version = "unspecified"

application {
    mainClass.set("net.thunderbird.cli.l10n.MainKt")
}

dependencies {
    implementation(libs.clikt)
}

codeCoverage {
    branchCoverage = 80
    lineCoverage = 80
}
