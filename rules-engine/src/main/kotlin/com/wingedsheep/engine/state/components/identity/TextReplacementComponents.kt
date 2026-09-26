package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TimestampComponent
import com.wingedsheep.engine.state.components.battlefield.chosenColor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
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
 * A single text replacement rule (e.g., "Elf" -> "Goblin", "Forest" -> "Island", "Red" -> "Blue").
 *
 * [duration] controls when the rule expires. Artificial Evolution-style creature-type
 * changes are [Duration.Permanent] ("this effect lasts indefinitely"); Crystal Spray's
 * color/land-type changes are [Duration.EndOfTurn] and are stripped during cleanup.
 */
@Serializable
data class TextReplacement(
    val fromWord: String,
    val toWord: String,
    val category: TextReplacementCategory,
    val duration: Duration = Duration.Permanent
)

/**
 * Stores text replacement rules for Layer 3 text-changing effects.
 *
 * Used by cards like Artificial Evolution ("replacing all instances of one creature type
 * with another") and Crystal Spray ("one color word with another or one basic land type
 * with another").
 *
 * Multiple replacements can stack (e.g., two Artificial Evolutions on the same permanent,
 * or a creature-type change plus a color-word change).
 */
@Serializable
data class TextReplacementComponent(
    val replacements: List<TextReplacement> = emptyList()
) : Component, TextReplacer {

    /**
     * Apply any subtype replacements (creature types AND basic land types — both are
     * subtypes) to a single subtype string. Chains across multiple matching rules.
     */
    fun applyToCreatureType(subtype: String): String {
        var result = subtype
        for (r in replacements) {
            if ((r.category == TextReplacementCategory.CREATURE_TYPE ||
                    r.category == TextReplacementCategory.BASIC_LAND_TYPE) &&
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

    /**
     * Apply any color-word replacements to a [Color]. Color words are matched
     * case-insensitively against the color's display name ("Red", "Blue", ...).
     * Returns the input color unchanged when no rule applies.
     */
    override fun replaceColor(color: Color): Color {
        var result = color
        for (r in replacements) {
            if (r.category == TextReplacementCategory.COLOR_WORD &&
                result.displayName.equals(r.fromWord, ignoreCase = true)) {
                result = Color.entries.firstOrNull { it.displayName.equals(r.toWord, ignoreCase = true) }
                    ?: result
            }
        }
        return result
    }

    fun withReplacement(replacement: TextReplacement): TextReplacementComponent =
        copy(replacements = replacements + replacement)
}

/**
 * Marks a permanent whose static ability changes every color word in the text of every spell and
 * permanent to the color chosen as it entered ([com.wingedsheep.sdk.scripting.ChangeAllColorWordsToChosenColor],
 * Swirl the Mists). Stamped by `StaticAbilityHandler` as the permanent enters; the chosen color is
 * read live off its `CastChoicesComponent`, so the order the entry choice and the stamp land in
 * doesn't matter. Only battlefield permanents are scanned, so the marker needs no cleanup.
 */
@Serializable
data object ChangesAllColorWordsComponent : Component

/**
 * The one read path for Layer 3 text-changing effects (CR 613.1c). An object's effective text
 * replacements are the global ones — a battlefield [ChangesAllColorWordsComponent] source's
 * "every color word becomes the chosen color", which reaches only spells and permanents — followed
 * by the object's own [TextReplacementComponent] rules (Crystal Spray, Artificial Evolution).
 *
 * Global rules come first: the object-specific rules are almost always the later effect (an instant
 * cast while the global source is out), and a later text change reads the text as already changed,
 * so "Blue" → "Red" on top of "every color word is blue" turns them all red.
 *
 * Every reader of text replacements goes through here instead of reading [TextReplacementComponent]
 * directly; a direct read silently ignores the global effect.
 */
object TextChanges {

    /** Effective replacements for [entityId]; null when nothing changes its text. */
    fun of(state: GameState, entityId: EntityId): TextReplacementComponent? {
        val own = state.getEntity(entityId)?.get<TextReplacementComponent>()
        val reachesGlobal = entityId in state.stack || entityId in state.getBattlefield()
        return merge(if (reachesGlobal) global(state) else emptyList(), own)
    }

    /**
     * Effective replacements for a card being cast right now: it is a spell from CR 601.2a, before
     * its modes and targets are chosen (601.2b–c), so the global rules apply to it even though the
     * card has not reached the stack yet.
     */
    fun forSpellBeingCast(state: GameState, cardId: EntityId): TextReplacementComponent? =
        merge(global(state), state.getEntity(cardId)?.get<TextReplacementComponent>())

    /**
     * The global rules in force: for each battlefield source (in timestamp order), every other
     * color word becomes its chosen color. Callers that look up many objects in one pass compute
     * this once and [merge] it with each object's own component.
     */
    fun global(state: GameState): List<TextReplacement> {
        val sources = state.getBattlefield().mapNotNull { id ->
            val container = state.getEntity(id) ?: return@mapNotNull null
            if (!container.has<ChangesAllColorWordsComponent>() || container.has<FaceDownComponent>()) return@mapNotNull null
            val color = container.chosenColor() ?: return@mapNotNull null
            (container.get<TimestampComponent>()?.timestamp ?: 0L) to color
        }
        if (sources.isEmpty()) return emptyList()
        return sources.sortedBy { it.first }.flatMap { (_, chosen) ->
            Color.entries.filter { it != chosen }.map {
                TextReplacement(it.displayName, chosen.displayName, TextReplacementCategory.COLOR_WORD)
            }
        }
    }

    /** [global] rules followed by the object's [own] rules; null when there are none. */
    fun merge(global: List<TextReplacement>, own: TextReplacementComponent?): TextReplacementComponent? = when {
        global.isEmpty() -> own
        own == null -> TextReplacementComponent(global)
        else -> TextReplacementComponent(global + own.replacements)
    }
}
