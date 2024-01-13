package com.github.starmel.composestatsplugin

import com.github.starmel.composestatsplugin.parser.ClassReportParser
import com.github.starmel.composestatsplugin.parser.model.ClassStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ClassParsingTest {

    val parser = ClassReportParser()
//    val fileContent = File("/Users/user/dev/compose-stats-plugin/src/test/resources/app_debug-classes.txt")
//        .readText()

    @Test
    fun `parse class with multiple unstable properties`() {
        parse(
            """
            unstable class AdProvider {
              unstable val context: Context
              unstable val lifecycle: Lifecycle
              unstable val preloaded: AdView
              <runtime stability> = Unstable
            }
        """.trimIndent()
        ) {
            assertClass(
                "AdProvider",
                stable = false,
                propertiesCount = 3
            ) {
                param(name = "context", type = "Context", stable = false)
                param(name = "lifecycle", type = "Lifecycle", stable = false)
                param(name = "preloaded", type = "AdView", stable = false)
            }
        }
    }

    @Test
    fun `parse class with no properties`() {
        parse(
            """
            stable class MainActivity {
              <runtime stability> = Stable
            }
        """.trimIndent()
        ) {
            assertClass(
                "MainActivity",
                stable = true,
                runtime = false,
                propertiesCount = 0
            )
        }
    }

    @Test
    fun `parse class with multiple stable and unstable properties`() {
        parse(
            """
            unstable class Photographer {
              stable val id: String
              stable val name: String
              stable val lastSeenOnline: String
              stable val avatar: Int
              stable val mainImage: Int
              stable val numOfFollowers: String
              stable val numOfFollowing: String
              unstable val tags: List<String>
              unstable val photos: Map<String, List<Int>>
              <runtime stability> = Unstable
            }
        """.trimIndent()
        ) {
            assertClass(
                "Photographer",
                stable = false,
                propertiesCount = 9
            ) {
                param(name = "id", type = "String", stable = true)
                param(name = "name", type = "String", stable = true)
                param(name = "lastSeenOnline", type = "String", stable = true)
                param(name = "avatar", type = "Int", stable = true)
                param(name = "mainImage", type = "Int", stable = true)
                param(name = "numOfFollowers", type = "String", stable = true)
                param(name = "numOfFollowing", type = "String", stable = true)
                param(name = "tags", type = "List<String>", stable = false)
                param(name = "photos", type = "Map<String, List<Int>>", stable = false)
            }
        }
    }

    @Test
    fun `parse class with multiple unstable properties 2`() {
        parse(
            """
            unstable class PhotographersViewModel {
              unstable val _photographers: MutableStateFlow<List<Photographer>>
              unstable val photographers: StateFlow<List<Photographer>>
              <runtime stability> = Unstable
            }
        """.trimIndent()
        ) {
            assertClass(
                "PhotographersViewModel",
                stable = false,
                propertiesCount = 2
            ) {
                param(name = "_photographers", type = "MutableStateFlow<List<Photographer>>", stable = false)
                param(name = "photographers", type = "StateFlow<List<Photographer>>", stable = false)
            }
        }
    }

    @Test
    fun `parse class with multiple unstable properties 3`() {
        parse(
            """
            unstable class HomeViewModel {
              runtime val postsRepository: PostsRepository
              unstable val viewModelState: MutableStateFlow<HomeViewModelState>
              unstable val uiState: StateFlow<HomeUiState>
              <runtime stability> = Unstable
            }
        """.trimIndent()
        ) {
            assertClass(
                "HomeViewModel",
                stable = false,
                propertiesCount = 3
            ) {
                param(name = "postsRepository", type = "PostsRepository", stable = false, mutable = false, runtime = true)
                param(name = "viewModelState", type = "MutableStateFlow<HomeViewModelState>", stable = false)
                param(name = "uiState", type = "StateFlow<HomeUiState>", stable = false)
            }
        }
    }

    @Test
    fun `parse class with multiple stable properties`() {
        parse(
            """
            stable class ErrorMessage {
              stable val id: Long
              stable val messageId: Int
              <runtime stability> = Stable
            }
        """.trimIndent()
        ) {
            assertClass(
                "ErrorMessage",
                stable = true,
                propertiesCount = 2
            ) {
                param(name = "id", type = "Long", stable = true)
                param(name = "messageId", type = "Int", stable = true)
            }
        }
    }

    @Test
    fun `parse class with multiple stable and unstable properties 2`() {

        val delegate = "$" + "delegate"

        parse(
            """
            unstable class ErrorObject {
              unstable val exception: Exception{ kotlin.TypeAliasesKt.Exception }
              unstable val interestsRepository$delegate: Lazy<FakeInterestsRepository>
              unstable val people$delegate: Lazy<List<String>>
              stable var requestCount: Int
              stable val publication: Publication?
              <runtime stability> = Unstable
            }
        """.trimIndent()
        ) {
            assertClass(
                "ErrorObject",
                stable = false,
                propertiesCount = 5
            ) {
                param(name = "exception", type = "Exception", stable = false)
                param(name = "interestsRepository$delegate", type = "Lazy<FakeInterestsRepository>", stable = false)
                param(name = "people$delegate", type = "Lazy<List<String>>", stable = false)
                param(name = "requestCount", type = "Int", stable = true, mutable = true)
                param(name = "publication", type = "Publication?", stable = true)
            }
        }
    }

    @Test
    fun `parse class with multiple runtime properties`() {
        parse(
            """
            unstable class JetnewsApplication {
              runtime var container: AppContainer
              <runtime stability> = Unstable
            }
        """.trimIndent()
        ) {
            assertClass(
                "JetnewsApplication",
                stable = false,
                propertiesCount = 1
            ) {
                param(name = "container", type = "AppContainer", stable = false, mutable = true, runtime = true)
            }
        }
    }


    private fun parse(
        string: String,
        assert: List<ClassStats>.() -> Unit
    ) {
        parser.parse(string).apply(assert)
    }
}

private fun List<ClassStats>.assertClass(
    name: String,
    stable: Boolean = false,
    runtime: Boolean = false,
    propertiesCount: Int? = null,
    funcsAssert: ClassPropertyAssertContext.() -> Unit = {}
) {
    val stats = find { it.name == name }
    try {
        assertNotNull(stats, "not found for name '$name'")

        assertEquals(stable, stats.isStable, "stable for class '$name'")
        assertEquals(runtime, stats.isRuntime, "runtime for class '$name'")
        if (propertiesCount != null) {
            assertEquals(propertiesCount, stats.properties.size, "propertiesCount for class '$name'")
        }

        ClassPropertyAssertContext(stats).apply(funcsAssert)
    } catch (e: AssertionError) {
        println(stats)
        throw e
    }
}

private data class ClassPropertyAssertContext(
    val functionStats: ClassStats
) {

    inline fun param(
        name: String,
        type: String,
        stable: Boolean = false,
        mutable: Boolean = false,
        runtime: Boolean = false,
    ) {

        val property = functionStats.properties.find {
            it.name == name
        }

        assertNotNull(property, "not found property name '$name'")
        assertEquals(stable, property.isStable, "stable for property '$name'")
        assertEquals(mutable, property.isMutable, "mutable for property '$name'")
        assertEquals(type, property.type, "type for property '$name'")
        assertEquals(runtime, property.isRuntime, "runtime for property '$name'")
    }
}