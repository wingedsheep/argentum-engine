package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.Serializable

/**
 * Category of text replacement for Layer 3 text-changing effects.
 */
@Serializable
enum class TextReplacementCategory {
    CREATURE_TYPE,
    COLOR_WORD,
    BASIC_LAND_TYPE
}

/**
 * A single text replacement rule (e.g., "Elf" -> "Goblin").
 */
@Serializable
data class TextReplacement(
    val fromWord: String,
    val toWord: String,
    val category: TextReplacementCategory
)

/**
 * Stores text replacement rules for Layer 3 text-changing effects.
 *
 * Used by cards like Artificial Evolution: "Change the text of target spell or permanent
 * by replacing all instances of one creature type with another."
 *
 * Multiple replacements can stack (e.g., two Artificial Evolutions on the same permanent).
 */
@Serializable
data class TextReplacementComponent(
    val replacements: List<TextReplacement> = emptyList()
) : Component, TextReplacer {

    fun applyToCreatureType(subtype: String): String {
        var result = subtype
        for (r in replacements) {
            if (r.category == TextReplacementCategory.CREATURE_TYPE &&
                result.equals(r.fromWord, ignoreCase = true)) {
                result = r.toWord
            }
        }
        return result
    }

    override fun replaceCreatureType(subtype: String): String = applyToCreatureType(subtype)

    fun applyToSubtype(subtype: Subtype): Subtype {
        val replaced = applyToCreatureType(subtype.value)
        return if (replaced == subtype.value) subtype else Subtype(replaced)
    }

    override fun replaceSubtype(subtype: Subtype): Subtype = applyToSubtype(subtype)

    fun withReplacement(replacement: TextReplacement): TextReplacementComponent =
        copy(replacements = replacements + replacement)
}
