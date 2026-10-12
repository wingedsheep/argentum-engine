package com.wingedsheep.engine.event

import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * The prominent banner shown while a player picks targets for a triggered ability — *what* the
 * targets are for ("Exile up to one target card from a graveyard. If a creature card was exiled
 * this way, …") rather than the bare "Choose target".
 *
 * Two texts are available, and neither is right every time:
 *
 *  - **The authored ability description** (`triggeredAbility { description = … }`) is the
 *    card's Oracle sentence. It reads correctly, but carries the trigger clause ("When this
 *    creature enters, …") — redundant while the ability is already triggering — and states a
 *    variable as its definition ("+X/+0, where X is …").
 *  - **The effect's runtime description** substitutes live values ("+3/+0"), which is why the
 *    banner used it. But for a step-composed sequence (an `Effects.Pipeline { }`, a `then`
 *    chain) it is the steps' text joined one by one — "Look at the targets. Put those cards into
 *    a hand. If those cards include …" — which is how pipeline-built cards ended up with a banner
 *    that read as implementation, not rules text.
 *
 * So the authored text wins whenever the card has one, minus its trigger clause. The runtime
 * text is kept only where it adds something the authored text can't: a live value substituted
 * into a single effect's own sentence (runtime text differs from the static text), and never
 * for a step-composed sequence whose text is generated rather than written.
 */
internal object TriggerTargetHint {

    /**
     * @param authored the ability's hand-written description, or null when it has none
     * @param effect the ability's effect
     * @param runtime [effect]'s runtime description with dynamic amounts resolved
     */
    fun choose(authored: String?, effect: Effect, runtime: String): String {
        val written = authored?.takeIf { it.isNotBlank() }?.let(::withoutTriggerClause) ?: return runtime
        val liveValues = runtime != effect.description
        return if (liveValues && !isStepComposed(effect)) runtime else written
    }

    /** A sequence whose text is its steps' generated text joined, not one hand-written sentence. */
    private fun isStepComposed(effect: Effect): Boolean =
        effect is CompositeEffect && effect.descriptionOverride == null

    private val triggerWords = listOf("When ", "Whenever ", "At ")

    /**
     * "When this creature enters, exile up to one target card…" → "Exile up to one target card…".
     * Only a leading trigger clause is cut, at its first comma; text that doesn't open with one is
     * returned unchanged.
     */
    fun withoutTriggerClause(text: String): String {
        val trimmed = text.trim()
        if (triggerWords.none { trimmed.startsWith(it) }) return trimmed
        val comma = trimmed.indexOf(", ")
        if (comma < 0) return trimmed
        val rest = trimmed.substring(comma + 2).trim()
        if (rest.isEmpty()) return trimmed
        return rest.replaceFirstChar { it.uppercaseChar() }
    }
}
