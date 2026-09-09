package net.thunderbird.cli.l10n

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

class L10nCli : CliktCommand(name = "l10n") {
    override fun help(context: Context): String = "Thunderbird localization maintenance"

    override fun run() = Unit
}

internal class CheckBranchCompatibilityCommand(
    private val checker: BranchCompatibilityChecker = BranchCompatibilityChecker(ProcessGitClient()),
) : CliktCommand(name = "check-branch-compatibility") {
    private val baseRef by option(help = "Base Git ref used to identify changed localization source files").required()
    private val headRef by option(help = "Head Git ref containing the proposed changes").default("HEAD")
    private val upstreamRef by option(help = "Closest upstream release-train Git ref")
    private val downstreamRefs by option(
        "--downstream-ref",
        help = "Downstream release-train Git ref to preserve compatibility with; may be repeated",
    ).multiple()
    private val allowTypoFix by option(
        help = "Allow text-only changes to keys and metadata still used downstream",
    ).flag()

    override fun help(context: Context): String = "Check localization source compatibility across release branches"

    override fun run() {
        if (upstreamRef == null && downstreamRefs.isEmpty()) {
            throw PrintMessage(
                "One of --upstream-ref or --downstream-ref is required",
                printError = true,
                statusCode = 2,
            )
        }

        val result =
            checker.check(
                CompatibilityOptions(
                    baseRef = baseRef,
                    headRef = headRef,
                    upstreamRef = upstreamRef,
                    downstreamRefs = downstreamRefs,
                    allowTypoFix = allowTypoFix,
                ),
            )
        if (result.failures.isNotEmpty()) {
            throw PrintMessage(result.renderFailure(), printError = true)
        }
        echo("Checked ${result.filesChecked} localization source files.")
    }
}
