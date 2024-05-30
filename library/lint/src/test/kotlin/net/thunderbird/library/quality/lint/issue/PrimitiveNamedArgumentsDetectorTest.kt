package net.thunderbird.library.quality.lint.issue

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import kotlin.test.Test

class PrimitiveNamedArgumentsDetectorTest {

    @Test
    fun `detect no issue for correct int example`() {
        val testFile = createCorrectTestCase("Int", "intAttribute", "42")

        lint().issues(PrimitiveNamedArgumentsIssue.ISSUE)
            .files(testFile)
            .run()
            .expectClean()
    }

    @Test
    fun `detect missing named argument for int primitive`() {
        lint().issues(PrimitiveNamedArgumentsIssue.ISSUE)
            .files(INT_EXAMPLE)
            .run()
            .expect(
                """
                src/com/example/Test.kt:4: Error: Primitive arguments should use named arguments [PrimitiveNamedArgumentsIssue]
                    foo(true)
                        ~~~
                1 errors, 0 warnings
                """.trimIndent(),
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/Test.kt line 4: Use named argument:
                @@ -4 +4
                -     foo(true)
                +     foo(intAttribute = true)
                """.trimIndent(),
            )
    }

    private companion object {
        private fun createCorrectTestCase(
            primitiveType: String,
            attributeName: String,
            attributeValue: String,
        ) =
            kotlin(
                """
                package com.example

                fun foo(${attributeName}: ${primitiveType}) {
                    println()
                }

                fun bar() {
                    foo(${attributeName} = ${attributeValue})
                }

                class PrimitiveTest {
                    fun main() {
                        foo(${attributeName} = ${attributeValue})
                    }
                }
            """.trimIndent(),
            )

        fun createIncorrectTestFile(primitiveType: String, primitiveValue: String) = kotlin(
            """
                package com.example

                fun foo(fooAttribute: primitiveType) {
                    println(booleanProperty)
                }

                fun bar() {
                    foo($primitiveValue)
            """.trimIndent(),
        )

        private val INT_EXAMPLE: TestFile = kotlin(
            """
                package com.example

                fun foo(intAttribute: Int) {
                    println(booleanProperty)
                }

                fun bar() {
                    foo(42)
                }


                class PrimitiveTest {
                    fun main() {
                        foo(42)
                    }
                }

            """.trimIndent(),
        )
    }
}
