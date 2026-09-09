package net.thunderbird.cli.l10n

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) =
    L10nCli()
        .subcommands(CheckBranchCompatibilityCommand())
        .main(args)
