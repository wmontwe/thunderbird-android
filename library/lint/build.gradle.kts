plugins {
    id(ThunderbirdPlugins.Library.jvm)
    id("thunderbird.quality.lint")
}

dependencies {
    compileOnly(libs.lint.api)
    testImplementation(libs.lint.checks)
    testImplementation(libs.lint.tests)
}
