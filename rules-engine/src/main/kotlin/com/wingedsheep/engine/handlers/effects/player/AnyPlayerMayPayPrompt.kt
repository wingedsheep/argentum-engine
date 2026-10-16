package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.SelfReferentialDescription
import com.wingedsheep.sdk.scripting.targets.resolveSelfNoun

/**
 * The prompt text for an "any player may [cost]" offer ([com.wingedsheep.sdk.scripting.effects.AnyPlayerMayPayEffect]),
 * built from the effect's own data rather than a fixed sentence: what the payment is, then what
 * paying does and what happens if nobody pays.
 *
 * Shared by the executor (first player asked) and the continuation resumer (every later player),
 * so both prompts read the same.
 *
 * - Innocent Traveler: "You may sacrifice a creature. If no one does, transform Innocent Traveler."
 * - Prowling Pangolin: "You may sacrifice two creatures. If you do, sacrifice Prowling Pangolin."
 * - Reservoir Kraken: "You may tap an untapped creature you control. If you do, tap Reservoir Kraken and …"
 */
object AnyPlayerMayPayPrompt {

    fun selection(
        cost: CostAtom,
        sourceName: String,
        consequence: Effect?,
        consequenceIfNonePaid: Effect?
    ): String = buildString {
        append("You may ${cost.description}.")
        if (consequence != null) append(" If you do, ${consequenceText(consequence, sourceName)}.")
        if (consequenceIfNonePaid != null) append(" If no one does, ${consequenceText(consequenceIfNonePaid, sourceName)}.")
    }

    /**
     * [effect]'s description with every reference to the source permanent ("this creature",
     * "this permanent", the self-noun placeholder) replaced by [sourceName], first letter lowered
     * so it reads as a clause, trailing period dropped.
     */
    private fun consequenceText(effect: Effect, sourceName: String): String {
        val raw = (effect as? SelfReferentialDescription)
            ?.let { resolveSelfNoun(it.descriptionTemplate, sourceName) }
            ?: effect.description
        return SELF_NOUNS.fold(raw) { text, noun -> text.replace(noun, sourceName) }
            .trim()
            .removeSuffix(".")
            .replaceFirstChar { it.lowercaseChar() }
    }

    private val SELF_NOUNS = listOf(
        "this creature", "this permanent", "this land", "this artifact",
        "this enchantment", "this planeswalker", "this battle"
    )
}
