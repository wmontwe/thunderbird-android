package net.thunderbird.cli.release

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int

class ReleaseCli : CliktCommand(
    help = "Release a version of Thunderbird",
) {
    private val dryRun by option(
        help = "Do not actually perform the release, just print the commands that would be executed",
    ).flag()

    private val type: ReleaseTypes by option(
        help = "The type of release to perform",
    ).enum<ReleaseTypes> {
        it.name.lowercase()
    }.prompt("Release type (daily, beta, production)")

    private val version: String by option(
        help = "The version to release",
    ).prompt("Version")

    private val versionCode: Int by option(
        help = "The version code to release",
    ).int().prompt("Version code")

    private val releaseNotes: String by option(
        help = "The release notes for this version",
    ).prompt("Release notes")

    override fun run() {
        echo("Releasing version of Thunderbird" + if (dryRun) " (dry run)" else "")
        echo()
        echo("Type: $type")
        echo("Version: $version")
        echo("Version code: $versionCode")
        echo("Release notes: $releaseNotes")
    }
}
