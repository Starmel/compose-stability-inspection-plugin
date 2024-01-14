package com.github.starmel.composestatsplugin

import com.github.starmel.composestatsplugin.report.parser.FunctionReportParser
import com.github.starmel.composestatsplugin.report.parser.model.FunctionStats
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FunctionParsingTest {

    val parser = FunctionReportParser()
    val fileContent = File("/Users/user/dev/compose-stats-plugin/src/test/resources/composables.txt")
        .readText()

    @Test
    fun testParsing() {

        val stats = parser.parse(fileContent)

        assertEquals(40, stats.size)

        with(stats) {
            assertFunction("defaultColor", returnType = "Color") {
                param(name = null, isThis = true, stable = true)
            }
            assertFunction("rippleAlpha", returnType = "RippleAlpha") {
                param(name = null, isThis = true, stable = true, unused = true)
            }
            assertFunction("FrontonTheme", restartable = true, skippable = true) {
                param(name = "isDarkTheme", stable = true)
                param(name = "content", stable = true)
            }
            assertFunction("color", getterFunction = true, returnType = "FrontonColors") {
                param(name = null, isThis = true, stable = true, unused = true)
            }
            assertFunction("typography", getterFunction = true, returnType = "FrontonTypography") {
                param(name = null, isThis = true, stable = true, unused = true)
            }
            assertFunction("FrontonButton", restartable = true, skippable = true) {
                param(name = "modifier", stable = true)
                param(name = "buttonText", stable = true)
                param(name = "buttonSecondaryText", stable = true)
                param(name = "buttonSize", stable = true)
                param(name = "buttonType", stable = true)
                param(name = "isRounded", stable = true)
                param(name = "buttonState", stable = true)
                param(name = "buttonIcon", stable = false)
                param(name = "buttonIconPosition", stable = true)
                param(name = "debounceInterval", stable = true)
                param(name = "onClick", stable = true)
            }
            assertFunction("FrontonButtonContainer", restartable = true, skippable = true) {
                param(name = "modifier", stable = true)
                param(name = "buttonState", stable = true)
                param(name = "backgroundColor", stable = true)
                param(name = "shape", stable = true)
                param(name = "borderStroke", stable = true)
                param(name = "onClick", stable = true)
                param(name = "indication", stable = true)
                param(name = "debounceInterval", stable = true)
                param(name = "content", stable = true)
            }
            assertFunction("FrontonInnerCompositeButton", restartable = true) {
                param(name = "modifier", stable = true)
                param(name = "backgroundColor", stable = true)
                param(name = "contentColor", stable = true)
                param(name = "rippleColor", stable = true)
                param(name = "shape", stable = true)
                param(name = "loaderInnerPadding", stable = true)
                param(name = "loaderIconSize", stable = true)
                param(name = "loaderStrokeWidth", stable = true)
                param(name = "borderStroke", stable = true)
                param(name = "buttonSize", stable = true)
                param(name = "buttonState", stable = true)
                param(name = "buttonText", stable = true)
                param(name = "buttonIcon", stable = false)
                param(name = "buttonIconPosition", stable = true)
                param(name = "buttonSecondaryText", stable = true)
                param(name = "debounceInterval", stable = true)
                param(name = "onClick", stable = true)
            }
        }
    }

    @Test
    fun `parse single line`() {
        parse("restartable skippable scheme(\"[androidx.compose.ui.UiComposable]\") fun RoundedHeaderPreview()\n") {
            assertFunction("RoundedHeaderPreview", restartable = true, skippable = true, paramsCount = 0)
        }
        parse("restartable skippable fun StatusBarColorProvider()") {
            assertFunction("StatusBarColorProvider", restartable = true, skippable = true, paramsCount = 0)
        }
    }

    @Test
    fun `parse function with multiple params`() {
        parse(
            "restartable skippable scheme(\"[androidx.compose.ui.UiComposable, [androidx.compose.ui.UiComposable]]\") fun TutorialOverlay(\n" +
                    "  stable content: Function3<Modifier, Composer, Int, Unit>\n" +
                    ")"
        ) {
            assertFunction("TutorialOverlay", restartable = true, skippable = true, paramsCount = 1) {
                param(name = "content", stable = true)
            }
        }
    }

    @Test
    fun `parse function with multiple params and multiple lines`() {
        parse(
            "restartable skippable scheme(\"[androidx.compose.ui.UiComposable]\") fun FollowerInfo(\n" +
                    "  stable text: String\n" +
                    "  stable number: String\n" +
                    ")"
        ) {
            assertFunction("FollowerInfo", restartable = true, skippable = true, paramsCount = 2) {
                param(name = "text", stable = true)
                param(name = "number", stable = true)
            }
        }
    }

    @Test
    fun `parse function with multiple params and multiple lines and unstable`() {
        parse(
            "restartable scheme(\"[androidx.compose.ui.UiComposable]\") fun ProfileHeader(\n" +
                    "  unstable photographer: Photographer\n" +
                    "  stable tutorialHighlightModifier: Modifier\n" +
                    ")"
        ) {
            assertFunction("ProfileHeader", restartable = true, paramsCount = 2) {
                param(name = "photographer", stable = false)
                param(name = "tutorialHighlightModifier", stable = true)
            }
        }
    }

    @Test
    fun `parse function with this param`() {
        parse(
            """
            fun isScrollingUp(
              stable <this>: LazyListState
            ): State<Boolean>
        """.trimIndent()
        ) {
            assertFunction(
                "isScrollingUp",
                restartable = false,
                skippable = false,
                paramsCount = 1,
                returnType = "State<Boolean>"
            ) {
                param(name = null, isThis = true, stable = true)
            }
        }
    }

    @Test
    fun `parse function with unstable param`() {
        parse(
            """
            restartable scheme("[androidx.compose.ui.UiComposable]") fun Feed(
              unstable photographersFlow: StateFlow<List<Photographer>>
              stable onSelected: Function1<Photographer, Unit>
            )
        """.trimIndent()
        ) {
            assertFunction(
                "Feed",
                restartable = true,
                skippable = false,
                paramsCount = 2,
                returnType = null
            ) {
                param(name = "photographersFlow", stable = false)
                param(name = "onSelected", stable = true)
            }
        }
    }

    private fun parse(
        string: String,
        assert: List<FunctionStats>.() -> Unit
    ) {
        parser.parse(string).apply(assert)
    }
}

private fun List<FunctionStats>.assertFunction(
    name: String,
    skippable: Boolean = false,
    restartable: Boolean = false,
    readOnly: Boolean = false,
    getterFunction: Boolean = false,
    returnType: String? = null,
    paramsCount: Int? = null,
    funcsAssert: FuncParameterAssertContext.() -> Unit = {}
) {
    val stats = find { it.name == name }
    try {
        assertNotNull(stats, "not found for name '$name'")

        assertEquals(skippable, stats.isSkippable, "skippable for fun '$name'")
        assertEquals(restartable, stats.isRestartable, "restartable for fun '$name'")
        assertEquals(readOnly, stats.isReadOnly, "readOnly for fun '$name'")
        assertEquals(getterFunction, stats.isGetterFunction, "getterFunction for fun '$name'")
        assertEquals(returnType, stats.returnType, "returnType for fun '$name'")

        if (paramsCount != null) {
            assertEquals(paramsCount, stats.parameters.size, "paramsCount for fun '$name'")
        }

        FuncParameterAssertContext(stats).apply(funcsAssert)
    } catch (e: AssertionError) {
        println(stats)
        throw e
    }
}

private data class FuncParameterAssertContext(
    val functionStats: FunctionStats
) {

    inline fun param(name: String?, stable: Boolean = false, isThis: Boolean = false, unused: Boolean = false) {
        val parameter = functionStats.parameters.find {
            if (name != null) {
                it.name == name
            } else {
                it.isThis == isThis
            }
        }
        assertNotNull(parameter, "not found parameter name '$name'")
        assertEquals(stable, parameter.isStable, "stable for parameter '$name'")
        assertEquals(isThis, parameter.isThis, "this for parameter '$name'")
        assertEquals(unused, parameter.isUnused, "unused for parameter '$name'")
    }
}