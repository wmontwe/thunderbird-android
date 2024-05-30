package net.thunderbird.library.quality.lint.issue

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression

object PrimitiveNamedArgumentsIssue {
    @JvmField
    val ISSUE: Issue = Issue.create(
        id = "PrimitiveNamedArguments",
        briefDescription = "Primitive arguments should use named arguments",
        explanation = "Using named arguments for primitive arguments makes the code more readable and less error-prone.",
        category = Category.CUSTOM_LINT_CHECKS,
        priority = 7,
        severity = Severity.ERROR,
        implementation = Implementation(
            PrimitiveNamedArgumentsDetector::class.java,
            Scope.JAVA_FILE_SCOPE,
        ),
    )
}

internal class PrimitiveNamedArgumentsDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(
        UCallExpression::class.java,
    )

    override fun createUastHandler(context: JavaContext): UElementHandler = PrimitiveNamedArgumentsVisitor(context)
}

private class PrimitiveNamedArgumentsVisitor(
    private val context: JavaContext,
) : UElementHandler() {

    override fun visitCallExpression(node: UCallExpression) {
        val arguments = node.valueArguments
        for (argument in arguments) {
            if (argument.getExpressionType()?.canonicalText == INT_TYPE) {
                val sourceCode = argument.sourcePsi?.text ?: continue

                val isNamed = sourceCode.contains("=")

                if (!isNamed) {
                    reportPrimitiveIssue(INT_TYPE, node, argument)
                }
            }
        }
    }

    private fun reportPrimitiveIssue(type: String, node: UCallExpression, argument: UExpression) {
        val parameter = getParameterForArgument(node, argument)
        val parameterName = parameter?.name ?: return
        val autoFix = createFix(node, argument, parameterName)

        context.report(
            issue = PrimitiveNamedArgumentsIssue.ISSUE,
            location = context.getNameLocation(node),
            message = """
                [${type.uppercase()}] Primitive arguments must use named arguments.
                Wrong usage: ${argument.asSourceString()} instead of $parameterName = ${argument.asSourceString()}.
            """,
            quickfixData = autoFix,
        )
    }

    private fun createFix(node: UCallExpression, argument: UExpression, parameterName: String): LintFix {
        val methodCall = node.asRenderString()
        val argumentValue = argument.sourcePsi?.text ?: ""
        val newMethodCall = methodCall.replace(argumentValue, "$parameterName = $argumentValue")

        return LintFix.create()
            .name("Use named argument")
            .replace()
            .text(methodCall)
            .with(newMethodCall)
            .autoFix()
            .build()
    }

    private fun getParameterForArgument(node: UCallExpression, argument: UExpression): PsiParameter? {
        val psiMethod = node.resolve() as? PsiMethod ?: return null
        val argumentIndex = node.valueArguments.indexOf(argument)
        if (argumentIndex < 0 || argumentIndex >= psiMethod.parameterList.parametersCount) return null

        return psiMethod.parameterList.parameters[argumentIndex]
    }

    private companion object {
        private const val BYTE_TYPE = "byte"
        private const val SHORT_TYPE = "short"
        private const val INT_TYPE = "int"
        private const val LONG_TYPE = "long"

        private const val FLOAT_TYPE = "float"
        private const val DOUBLE_TYPE = "double"

        private const val UBYTE_TYPE = "ubyte"
        private const val USHORT_TYPE = "ushort"
        private const val UINT_TYPE = "uint"
        private const val ULONG_TYPE = "ulong"

        private const val BOOLEAN_TYPE = "boolean"
        private const val CHAR_TYPE = "char"
        private const val STRING_TYPE = "string"
    }
}
