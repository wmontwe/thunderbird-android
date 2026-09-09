package net.thunderbird.cli.l10n

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test

class ProcessGitClientTest {
    private val repository = Files.createTempDirectory("l10n-cli-git").toFile()

    @AfterTest
    fun tearDown() {
        repository.deleteRecursively()
    }

    @Test
    fun `reads files and changed paths from Git refs`() {
        runGit("init", "--quiet")
        runGit("config", "user.name", "Test")
        runGit("config", "user.email", "test@example.invalid")
        runGit("config", "commit.gpgsign", "false")
        val path = "feature/example/src/main/res/values/strings.xml"
        repository.resolve(path).apply {
            parentFile.mkdirs()
            writeText("<resources><string name=\"example\">Base</string></resources>\n")
        }
        runGit("add", path)
        runGit("commit", "--quiet", "--message", "base")
        val baseRef = runGit("rev-parse", "HEAD").trim()

        repository.resolve(path).writeText(
            "<resources><string name=\"example\">Head</string></resources>\n",
        )
        runGit("add", path)
        runGit("commit", "--quiet", "--message", "head")
        val headRef = runGit("rev-parse", "HEAD").trim()
        val testSubject = ProcessGitClient(repository)

        assertThat(testSubject.changedFiles(baseRef, headRef)).containsExactly(path)
        assertThat(testSubject.readFile(baseRef, path))
            .isEqualTo("<resources><string name=\"example\">Base</string></resources>\n")
    }

    private fun runGit(vararg arguments: String): String {
        val process =
            ProcessBuilder(listOf("git", "-C", repository.absolutePath) + arguments)
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0) { output }
        return output
    }
}
