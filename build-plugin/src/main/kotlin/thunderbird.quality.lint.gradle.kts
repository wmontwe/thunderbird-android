import com.android.build.api.dsl.Lint

project.plugins.withId("com.android.application") {
    configure<Lint> {
        configure()
    }
}

project.plugins.withId("com.android.library") {
    configure<Lint> {
        configure()
    }
}

project.plugins.withId("com.android.lint") {
    configure<Lint> {
        configure()
    }
}

fun Lint.configure() {
    xmlReport = true
    checkDependencies = true
}


