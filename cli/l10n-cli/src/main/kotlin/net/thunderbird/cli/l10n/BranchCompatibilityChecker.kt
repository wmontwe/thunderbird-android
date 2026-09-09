package net.thunderbird.cli.l10n

import java.io.File

internal class BranchCompatibilityChecker(
    private val gitClient: GitClient,
    private val resourceParser: ResourceParser = ResourceParser(),
) {
    fun check(options: CompatibilityOptions): CompatibilityResult {
        require(options.upstreamRef != null || options.downstreamRefs.isNotEmpty()) {
            "One of upstreamRef or downstreamRefs is required"
        }

        val files =
            gitClient
                .changedFiles(options.baseRef, options.headRef)
                .filter(::isLocalizationSourceFile)
        val failures = buildList {
            files.forEach { path ->
                options.upstreamRef?.let { upstreamRef ->
                    addAll(checkAgainstUpstream(path, options.baseRef, options.headRef, upstreamRef))
                }
                options.downstreamRefs.forEach { downstreamRef ->
                    addAll(
                        checkAgainstDownstream(
                            path = path,
                            baseRef = options.baseRef,
                            headRef = options.headRef,
                            downstreamRef = downstreamRef,
                            allowTypoFix = options.allowTypoFix,
                        ),
                    )
                }
            }
        }
        return CompatibilityResult(filesChecked = files.size, failures = failures)
    }

    private fun checkAgainstUpstream(
        path: String,
        baseRef: String,
        headRef: String,
        upstreamRef: String,
    ): List<String> =
        if (path.endsWith(".xml")) {
            checkResourceAgainstUpstream(path, baseRef, headRef, upstreamRef)
        } else {
            checkTextAgainstUpstream(path, baseRef, headRef, upstreamRef)
        }

    private fun checkResourceAgainstUpstream(
        path: String,
        baseRef: String,
        headRef: String,
        upstreamRef: String,
    ): List<String> =
        catchInvalidResource {
            val baseEntries = parseResource(path, baseRef)
            val headEntries = parseResource(path, headRef)
            val upstreamEntries = parseResource(path, upstreamRef)

            changedKeys(baseEntries, headEntries).sorted().mapNotNull { key ->
                val headEntry = headEntries[key]
                val upstreamEntry = upstreamEntries[key]
                when {
                    headEntry == null && upstreamEntry != null ->
                        "$path: removed $key, but it still exists in $upstreamRef. " +
                            "Release-train branches must not remove localization sources independently."

                    headEntry != null && upstreamEntry == null ->
                        "$path: changed or added $key, but it does not exist in $upstreamRef. " +
                            "Land localization source changes on the closest upstream branch first."

                    headEntry != upstreamEntry ->
                        "$path: $key differs from $upstreamRef. " +
                            "Release-train localization sources must match the closest upstream branch."

                    else -> null
                }
            }
        }

    private fun checkTextAgainstUpstream(
        path: String,
        baseRef: String,
        headRef: String,
        upstreamRef: String,
    ): List<String> {
        val baseContent = gitClient.readFile(baseRef, path)
        val headContent = gitClient.readFile(headRef, path)
        if (baseContent == headContent || headContent == gitClient.readFile(upstreamRef, path)) {
            return emptyList()
        }
        return listOf(
            "$path: source store listing text differs from $upstreamRef. " +
                "Land localization source changes on the closest upstream branch first.",
        )
    }

    private fun checkAgainstDownstream(
        path: String,
        baseRef: String,
        headRef: String,
        downstreamRef: String,
        allowTypoFix: Boolean,
    ): List<String> =
        if (path.endsWith(".xml")) {
            checkResourceAgainstDownstream(path, baseRef, headRef, downstreamRef, allowTypoFix)
        } else {
            checkTextAgainstDownstream(path, baseRef, headRef, downstreamRef, allowTypoFix)
        }

    private fun checkResourceAgainstDownstream(
        path: String,
        baseRef: String,
        headRef: String,
        downstreamRef: String,
        allowTypoFix: Boolean,
    ): List<String> =
        catchInvalidResource {
            val baseEntries = parseResource(path, baseRef)
            val headEntries = parseResource(path, headRef)
            val downstreamEntries = parseResource(path, downstreamRef)

            changedKeys(baseEntries, headEntries).sorted().mapNotNull { key ->
                val headEntry = headEntries[key]
                val downstreamEntry = downstreamEntries[key] ?: return@mapNotNull null
                when {
                    headEntry == null -> null

                    !headEntry.isStructurallyCompatibleWith(downstreamEntry) ->
                        "$path: changed $key incompatibly with $downstreamRef. " +
                            "Use a new source key when placeholders or plural forms change."

                    headEntry.serialized != downstreamEntry.serialized && !allowTypoFix ->
                        "$path: changed existing source text for $key while it is still used by $downstreamRef. " +
                            "Use a new key for meaning changes or mark a typo-only correction explicitly."

                    else -> null
                }
            }
        }

    private fun checkTextAgainstDownstream(
        path: String,
        baseRef: String,
        headRef: String,
        downstreamRef: String,
        allowTypoFix: Boolean,
    ): List<String> {
        val baseContent = gitClient.readFile(baseRef, path)
        val headContent = gitClient.readFile(headRef, path)
        if (baseContent == headContent || headContent == null) return emptyList()

        val downstreamContent = gitClient.readFile(downstreamRef, path)
        return if (downstreamContent != null && headContent != downstreamContent && !allowTypoFix) {
            listOf(
                "$path: changed existing source store listing text while it still exists in $downstreamRef. " +
                    "Use a new file or mark a typo-only correction explicitly.",
            )
        } else {
            emptyList()
        }
    }

    private fun parseResource(path: String, ref: String): Map<String, ResourceEntry> =
        resourceParser.parse(gitClient.readFile(ref, path), path, ref)

    private fun changedKeys(
        baseEntries: Map<String, ResourceEntry>,
        headEntries: Map<String, ResourceEntry>,
    ): Set<String> =
        (baseEntries.keys + headEntries.keys).filterTo(linkedSetOf()) { key ->
            baseEntries[key] != headEntries[key]
        }

    private fun ResourceEntry.isStructurallyCompatibleWith(other: ResourceEntry): Boolean =
        placeholders == other.placeholders && pluralQuantities == other.pluralQuantities

    private fun isLocalizationSourceFile(path: String): Boolean =
        RESOURCE_SOURCE_SUFFIXES.any(path::endsWith) ||
            (STORE_SOURCE_PREFIXES.any(path::startsWith) && File(path).name in STORE_SOURCE_NAMES)

    private companion object {
        val RESOURCE_SOURCE_SUFFIXES =
            listOf(
                "/res/values/strings.xml",
                "/res/values/plurals.xml",
                "/composeResources/values/strings.xml",
                "/composeResources/values/plurals.xml",
            )
        val STORE_SOURCE_PREFIXES =
            listOf(
                "app-metadata/com.fsck.k9/en-US/",
                "app-metadata/net.thunderbird.android.beta/en-US/",
                "app-metadata/net.thunderbird.android/en-US/",
            )
        val STORE_SOURCE_NAMES = setOf("full_description.txt", "short_description.txt", "title.txt")
    }
}

private fun catchInvalidResource(block: () -> List<String>): List<String> =
    try {
        block()
    } catch (exception: InvalidResourceFile) {
        listOf(exception.message.orEmpty())
    }

internal data class CompatibilityOptions(
    val baseRef: String,
    val headRef: String = "HEAD",
    val upstreamRef: String? = null,
    val downstreamRefs: List<String> = emptyList(),
    val allowTypoFix: Boolean = false,
)

internal data class CompatibilityResult(
    val filesChecked: Int,
    val failures: List<String>,
) {
    fun renderFailure(): String = buildString {
        appendLine("Incompatible localization source changes were found.")
        appendLine()
        failures.forEach { appendLine("- $it") }
        appendLine()
        append("Fix: use a new key for incompatible changes or mark a typo-only correction explicitly.")
    }
}
