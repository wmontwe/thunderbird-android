plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.common"
    resourcePrefix = "core_ui_common_"
}

dependencies {
    testImplementation(projects.core.ui.compose.testing)
    testImplementation(projects.core.ui.compose.designsystem)
}
