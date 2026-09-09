package net.thunderbird.cli.l10n

import java.io.File

internal interface GitClient {
    fun changedFiles(baseRef: String, headRef: String): List<String>

    fun readFile(ref: String, path: String): String?
}

internal class ProcessGitClient(
    private val repositoryRoot: File = File("."),
) : GitClient {
    override fun changedFiles(baseRef: String, headRef: String): List<String> =
        runGit("diff", "--name-only", "$baseRef...$headRef")
            .output
            .lineSequence()
            .filter { it.isNotBlank() }
            .toList()

    override fun readFile(ref: String, path: String): String? {
        val result = runGit("show", "$ref:$path", requireSuccess = false)
        return result.output.takeIf { result.exitCode == 0 }
    }

    private fun runGit(vararg arguments: String, requireSuccess: Boolean = true): ProcessResult {
        val process =
            ProcessBuilder(listOf("git", "-C", repositoryRoot.absolutePath) + arguments)
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        check(!requireSuccess || exitCode == 0) {
            "Git command failed (${arguments.joinToString(" ")}): ${output.trim()}"
        }
        return ProcessResult(exitCode, output)
    }
}

private data class ProcessResult(val exitCode: Int, val output: String)
