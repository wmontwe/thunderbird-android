package net.thunderbird.library.quality.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue
import net.thunderbird.library.quality.lint.issue.PrimitiveNamedArgumentsIssue

class LintIssueRegistry : IssueRegistry() {

    override val issues: List<Issue> = listOf(
        PrimitiveNamedArgumentsIssue.ISSUE,
    )

    override val api: Int = CURRENT_API

    override val minApi: Int = 14
}
