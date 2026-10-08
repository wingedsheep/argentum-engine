package com.wingedsheep.sdk.surface

import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.SerialName
import java.io.File
import java.lang.reflect.Modifier
import java.util.jar.JarFile
import kotlin.reflect.KClass
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

/**
 * The SDK's sealed vocabulary as data: every sealed *root* in `mtg-sdk` (a sealed class or
 * interface no other sealed SDK type extends — `Effect`, `CardPredicate`, `Condition`, …) and the
 * concrete leaves under it. Shared by [SdkSurfaceBaselineTest] (the review prompt on a new leaf)
 * and [SdkTailReport] (`just sdk-tail`).
 *
 * Discovery walks the compiled `mtg-sdk` main classes rather than a hand-kept list of families, so
 * a new sealed family is covered the moment it compiles.
 */
internal object SdkSurface {

    data class Field(val name: String, val type: String) {
        override fun toString() = "$name: $type"
    }

    data class Leaf(
        /** Name relative to its package, nesting kept: `CardPredicate.PowerEqualsX`, `CompositeEffect`. */
        val name: String,
        val family: String,
        val fields: List<Field>,
        /** The class discriminator a card's JSON carries for this leaf. */
        val serialName: String,
        val packageName: String,
    ) {
        val simpleName: String get() = name.substringAfterLast('.')
        val key: String get() = "$name — $family"
    }

    val leaves: List<Leaf> by lazy { discover() }

    val byFamily: Map<String, List<Leaf>> by lazy { leaves.groupBy { it.family } }

    /** Leaves in [leaf]'s family ranked by likeness: identical fields first, then shared name words. */
    fun neighbours(leaf: Leaf, limit: Int = 5): List<Pair<Leaf, Int>> {
        val words = nameWords(leaf)
        return byFamily[leaf.family].orEmpty()
            .filter { it.name != leaf.name }
            .map { other -> other to (2 * leaf.fields.intersect(other.fields.toSet()).size + words.intersect(nameWords(other)).size) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<Leaf, Int>> { it.second }.thenBy { it.first.name })
            .take(limit)
    }

    /** Camel-case words of the leaf's simple name, minus the words of its family name ("Effect", …). */
    fun nameWords(leaf: Leaf): Set<String> =
        camelWords(leaf.simpleName) - camelWords(leaf.family.substringAfterLast('.'))

    private fun camelWords(s: String): Set<String> =
        Regex("[A-Z]+(?![a-z])|[A-Z]?[a-z]+|\\d+").findAll(s).map { it.value.lowercase() }.toSet()

    private const val SDK_PACKAGE = "com.wingedsheep.sdk"

    private fun discover(): List<Leaf> {
        val sealedTypes = sdkClassNames()
            .mapNotNull { runCatching { Class.forName(it, false, Effect::class.java.classLoader).kotlin }.getOrNull() }
            .filter { runCatching { it.isSealed }.getOrDefault(false) }
        val sealedSet = sealedTypes.toSet()
        val roots = sealedTypes.filter { type ->
            type.allSupertypes.none { (it.classifier as? KClass<*>) in sealedSet }
        }
        return roots.flatMap { root ->
            val family = relativeName(root)
            leavesOf(root).map { leafClass ->
                Leaf(
                    name = relativeName(leafClass),
                    family = family,
                    fields = leafClass.primaryConstructor?.parameters.orEmpty()
                        .map { Field(it.name ?: "_", simpleType(it.type.toString())) },
                    serialName = leafClass.findAnnotation<SerialName>()?.value ?: leafClass.qualifiedName!!,
                    packageName = leafClass.java.`package`.name,
                )
            }
        }.distinctBy { it.key }.sortedWith(compareBy({ it.family }, { it.name }))
    }

    private fun leavesOf(root: KClass<*>): Set<KClass<*>> {
        val leaves = mutableSetOf<KClass<*>>()
        val queue = ArrayDeque(root.sealedSubclasses)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current.sealedSubclasses.isNotEmpty()) {
                queue.addAll(current.sealedSubclasses)
                continue
            }
            val java = current.java
            if (java.isInterface || Modifier.isAbstract(java.modifiers)) continue
            leaves += current
        }
        return leaves
    }

    private fun relativeName(k: KClass<*>): String =
        k.qualifiedName!!.removePrefix(k.java.`package`.name + ".")

    /** `kotlin.collections.List<com.wingedsheep.sdk.X>?` → `List<X>?`. */
    private fun simpleType(t: String): String = t.replace(Regex("""\b[a-z][\w]*\.(?:[a-z][\w]*\.)*"""), "")

    /** Fully qualified names of every top-level and nested class compiled into `mtg-sdk` main. */
    private fun sdkClassNames(): List<String> {
        val location = File(Effect::class.java.protectionDomain.codeSource.location.toURI())
        val entries: List<String> = if (location.isDirectory) {
            location.walkTopDown().filter { it.isFile }.map { it.relativeTo(location).invariantSeparatorsPath }.toList()
        } else {
            JarFile(location).use { jar -> jar.entries().toList().map { it.name } }
        }
        return entries
            .filter { it.endsWith(".class") && !it.endsWith("module-info.class") }
            .map { it.removeSuffix(".class").replace('/', '.') }
            .filter { it.startsWith(SDK_PACKAGE) }
            // Skip compiler-generated classes: `$$serializer`, lambdas (`$1`). Neither can be sealed.
            .filterNot { name -> name.substringAfterLast('.').split('$').drop(1).any { it.isEmpty() || it[0].isDigit() } }
    }
}
