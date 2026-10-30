package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.selfNounToken
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Tap/Untap Effects
// =============================================================================

/**
 * Tap/Untap target effect.
 * "Tap target creature" or "Untap target creature"
 */
@SerialName("TapUntap")
@Serializable
data class TapUntapEffect(
    val target: EffectTarget,
    val tap: Boolean = true
) : Effect, SelfReferentialDescription {
    // Self routes through the self-noun token: "tap this creature" is wrong on a land (Multiversal
    // Passage's "if you don't, it enters tapped" read "tap this creature").
    override val descriptionTemplate: String = "${if (tap) "Tap" else "Untap"} ${target.selfNounToken}"
    override val description: String = defaultResolvedDescription
}

/**
 * "This permanent enters tapped" — the rider of an as-enters replacement, run inside
 * [com.wingedsheep.sdk.scripting.OnEnterRun]: the reveal lands' "if you don't, this land enters
 * tapped" (Auntie's Hovel, Game Trail, the snarls), Multiversal Passage's "if you don't [pay 2
 * life], it enters tapped".
 *
 * An entry modification, not a tap. The permanent is put onto the battlefield tapped as part of
 * the entry event, so no "becomes tapped" event is emitted, and a "[permanents] enter untapped"
 * replacement (The Wandering Minstrel, Spelunking) wins and the permanent stays untapped
 * (CR 616.1 — its controller orders the two replacements, and untapped is the only outcome worth
 * choosing). [TapUntapEffect] on `EffectTarget.Self` does neither.
 */
@SerialName("EnterTapped")
@Serializable
data object EnterTappedEffect : Effect, SelfReferentialDescription {
    override val descriptionTemplate: String = "${EffectTarget.Self.selfNounToken} enters tapped"
    override val description: String get() = defaultResolvedDescription
}

/**
 * Tap or untap all entities in a named collection.
 * Used for effects that let a player choose permanents to tap/untap from a selection.
 * "Untap up to two lands" (after Gather → Select → TapUntapCollection)
 */
@SerialName("TapUntapCollection")
@Serializable
data class TapUntapCollectionEffect(
    val collectionName: String,
    val tap: Boolean = true
) : Effect {
    override val description: String = "${if (tap) "Tap" else "Untap"} each of those permanents"
}

/**
 * Phase out the target permanent (and anything attached to it).
 *
 * "Phased out" is a per-permanent status (Rule 702.26): while a permanent is phased
 * out it's treated as though it doesn't exist, and it phases back in before its
 * controller untaps during their next untap step. The permanent keeps its tapped
 * state, counters, and attachments through the cycle; phasing is not a zone change,
 * so it doesn't trigger enters/leaves abilities.
 *
 * Used as the "suffer" effect of a pay-or-phase trigger
 * (e.g. Vaporous Djinn: "phases out unless you pay {U}{U}").
 *
 * @property cantPhaseIn A window during which the permanent can't phase in — its untap-step
 *   phase-in is skipped until the window ends, after which it phases in at its controller's *next*
 *   untap step (the window ending never phases anything in, which is what separates this from
 *   [PhaseOutUntilLeavesEffect]). Null for ordinary phasing. Two windows are supported:
 *   - [Duration.WhileYouControlSource] — "It can't phase in for as long as you control this Saga"
 *     (The Phasing of Zhalfir). Ends one-way (CR 611.2b) the moment the source leaves the
 *     battlefield or changes controller.
 *   - [Duration.EndOfYourNextTurn] — "Until the end of your next turn, they can't phase in"
 *     (Teferi, Timeless Voyager). Ends at the cleanup step of the effect controller's next turn.
 */
@SerialName("PhaseOut")
@Serializable
data class PhaseOutEffect(
    val target: EffectTarget = EffectTarget.Self,
    val cantPhaseIn: Duration? = null
) : Effect {
    init {
        require(cantPhaseIn == null || cantPhaseIn is Duration.WhileYouControlSource ||
            cantPhaseIn == Duration.EndOfYourNextTurn) {
            "PhaseOutEffect.cantPhaseIn supports WhileYouControlSource and EndOfYourNextTurn, not $cantPhaseIn"
        }
    }

    override val description: String = buildString {
        append("${target.description} phases out")
        when (cantPhaseIn) {
            null -> {}
            is Duration.WhileYouControlSource -> append(". It can't phase in ${cantPhaseIn.description}")
            else -> append(". ${cantPhaseIn.description.replaceFirstChar { it.uppercase() }}, it can't phase in")
        }
    }
}

/**
 * Phase a target permanent out **indefinitely**, linked to the effect's source: it stays phased
 * out (it does not phase in at its controller's untap step) until the source leaves the
 * battlefield, at which point a [PhaseInLinkedToSourceEffect] on the source's leaves trigger phases
 * it back in. The structural analogue of `ExileUntilLeavesEffect`, but with phasing (Oubliette).
 *
 * @property tapOnPhaseIn Tap the permanent when it phases back in (Oubliette: "Tap that creature as
 *   it phases in this way").
 */
@SerialName("PhaseOutUntilLeaves")
@Serializable
data class PhaseOutUntilLeavesEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val tapOnPhaseIn: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("${target.description} phases out until this leaves the battlefield")
        if (tapOnPhaseIn) append(" (tapped on phase-in)")
    }
}

/**
 * Phase in every permanent that was phased out "until source leaves" by the effect's source
 * (matched on the stored source link). Paired with [PhaseOutUntilLeavesEffect] on the source's
 * leaves-battlefield trigger (Oubliette).
 */
@SerialName("PhaseInLinkedToSource")
@Serializable
data object PhaseInLinkedToSourceEffect : Effect {
    override val description: String = "phase in the permanents this phased out"
}
