package com.wingedsheep.engine.hygiene

import com.wingedsheep.engine.core.ContinuationFrame
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.Component
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerializersModuleCollector
import java.io.File
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

/**
 * Guards principle §2.4: the engine must be able to serialize a paused `GameState` — which
 * means every concrete [GameAction], [GameEvent], [ContinuationFrame], [PendingDecision],
 * [DecisionResponse], and [Component] must be polymorphically registered in
 * [engineSerializersModule].
 *
 * The old bug: the list in `Serialization.kt` was maintained manually. Adding a new continuation
 * or component without remembering to register it silently broke round-trip of any state that
 * happened to contain that type. This test walks the actual class hierarchies and asserts that
 * `dumpTo` reports every leaf — so new types fail the test instead of production.
 *
 * For the five sealed hierarchies we use Kotlin reflection (`sealedSubclasses` walked to the
 * leaves). [Component] is an open interface, so we walk the compiled class output under
 * `build/classes/kotlin/main` and collect every concrete class assignable to [Component].
 *
 * If this test fails, the fix is almost always "add a `subclass(X::class)` line to the matching
 * `polymorphic` block in `Serialization.kt`."
 */
@OptIn(ExperimentalSerializationApi::class)
class SerializationPolymorphicRegistrationTest : FunSpec({

    test("every sealed GameAction subclass is polymorphically registered") {
        assertAllLeavesRegistered(GameAction::class)
    }

    test("every sealed GameEvent subclass is polymorphically registered") {
        assertAllLeavesRegistered(GameEvent::class)
    }

    test("every sealed ContinuationFrame subclass is polymorphically registered") {
        assertAllLeavesRegistered(ContinuationFrame::class)
    }

    test("every sealed PendingDecision subclass is polymorphically registered") {
        assertAllLeavesRegistered(PendingDecision::class)
    }

    test("every sealed DecisionResponse subclass is polymorphically registered") {
        assertAllLeavesRegistered(DecisionResponse::class)
    }

    test("every Component implementation is polymorphically registered") {
        val registered = registeredSubclassesFor(Component::class)
        val discovered = findAllComponentImplementations()
        val missing = (discovered - registered)
            .map { it.qualifiedName ?: it.java.name }
            .sorted()
        withClue(
            "Missing Component subclass() registrations in engineSerializersModule " +
                "(add them to the `polymorphic(Component::class)` block in Serialization.kt):\n" +
                missing.joinToString("\n") { "  - $it" }
        ) {
            missing.shouldBeEmpty()
        }
    }
}) {
    companion object {

        private fun <T : Any> assertAllLeavesRegistered(base: KClass<T>) {
            val registered = registeredSubclassesFor(base)
            val discovered = findLeafSealedSubclasses(base)

            val missing = (discovered - registered)
                .map { it.qualifiedName ?: it.java.name }
                .sorted()
            val unknown = (registered - discovered)
                .map { it.qualifiedName ?: it.java.name }
                .sorted()

            val problems = buildString {
                if (missing.isNotEmpty()) {
                    appendLine(
                        "Missing polymorphic registrations for ${base.simpleName} " +
                            "(add to `polymorphic(${base.simpleName}::class)` in Serialization.kt):"
                    )
                    missing.forEach { appendLine("  - $it") }
                }
                if (unknown.isNotEmpty()) {
                    appendLine(
                        "Stale polymorphic registrations for ${base.simpleName} " +
                            "(classes no longer exist or no longer extend ${base.simpleName}):"
                    )
                    unknown.forEach { appendLine("  - $it") }
                }
            }
            if (problems.isNotEmpty()) error(problems)
        }

        /**
         * Asks the [engineSerializersModule] to dump every registration it knows about and
         * returns the concrete subclasses whose base matches [base].
         */
        private fun <T : Any> registeredSubclassesFor(base: KClass<T>): Set<KClass<*>> {
            val result = mutableSetOf<KClass<*>>()
            engineSerializersModule.dumpTo(object : SerializersModuleCollector {
                override fun <B : Any, Sub : B> polymorphic(
                    baseClass: KClass<B>,
                    actualClass: KClass<Sub>,
                    actualSerializer: KSerializer<Sub>
                ) {
                    if (baseClass == base) result.add(actualClass)
                }

                override fun <U : Any> contextual(kClass: KClass<U>, serializer: KSerializer<U>) = Unit

                override fun <U : Any> contextual(
                    kClass: KClass<U>,
                    provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>
                ) = Unit

                override fun <B : Any> polymorphicDefaultSerializer(
                    baseClass: KClass<B>,
                    defaultSerializerProvider: (value: B) -> SerializationStrategy<B>?
                ) = Unit

                override fun <B : Any> polymorphicDefaultDeserializer(
                    baseClass: KClass<B>,
                    defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<B>?
                ) = Unit
            })
            return result
        }

        /**
         * Walks a sealed hierarchy to its leaves — concrete, non-abstract classes that no
         * other class in the sealed hierarchy extends.
         */
        private fun <T : Any> findLeafSealedSubclasses(base: KClass<T>): Set<KClass<*>> {
            require(base.isSealed) { "${base.qualifiedName} must be sealed to be walked reflectively" }
            val leaves = mutableSetOf<KClass<*>>()
            val queue: ArrayDeque<KClass<*>> = ArrayDeque(base.sealedSubclasses)
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val children = current.sealedSubclasses
                if (children.isNotEmpty()) {
                    queue.addAll(children)
                    continue
                }
                val javaClass = current.java
                if (javaClass.isInterface) continue
                if (Modifier.isAbstract(javaClass.modifiers)) continue
                leaves.add(current)
            }
            return leaves
        }

        /**
         * [Component] is an open interface (not sealed), so reflection alone can't enumerate
         * its implementations. Instead we walk the compiled class output of this module and
         * load every class that is assignable to [Component].
         *
         * We intentionally initialize=false so loading doesn't fire static initializers.
         */
        private fun findAllComponentImplementations(): Set<KClass<out Component>> {
            val classesRoot = listOf(
                File("build/classes/kotlin/main"),
                File("rules-engine/build/classes/kotlin/main")
            ).firstOrNull { it.isDirectory }
                ?: error(
                    "Could not locate rules-engine/build/classes/kotlin/main — run `just build` " +
                        "or `./gradlew :rules-engine:compileKotlin` before running this test."
                )

            val classLoader = Thread.currentThread().contextClassLoader
            val result = mutableSetOf<KClass<out Component>>()
            classesRoot.walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .forEach { file ->
                    val binaryName = file.relativeTo(classesRoot).path
                        .replace(File.separatorChar, '.')
                        .removeSuffix(".class")

                    // Skip Kotlin-generated support classes — serializers, companions, WhenMappings.
                    if (binaryName.endsWith("\$\$serializer")) return@forEach
                    if (binaryName.endsWith("\$Companion")) return@forEach
                    if (binaryName.endsWith("\$WhenMappings")) return@forEach
                    if (binaryName.endsWith("Kt")) return@forEach  // top-level function files
                    // Skip synthetic lambda / closure classes.
                    if (binaryName.matches(Regex(".*\\$\\d+$"))) return@forEach

                    val clazz = runCatching {
                        Class.forName(binaryName, false, classLoader)
                    }.getOrNull() ?: return@forEach

                    if (!Component::class.java.isAssignableFrom(clazz)) return@forEach
                    if (clazz == Component::class.java) return@forEach
                    if (clazz.isInterface) return@forEach
                    if (Modifier.isAbstract(clazz.modifiers)) return@forEach
                    if (clazz.isSynthetic) return@forEach
                    if (clazz.isAnonymousClass) return@forEach

                    @Suppress("UNCHECKED_CAST")
                    result.add(clazz.kotlin as KClass<out Component>)
                }
            return result
        }
    }
}
