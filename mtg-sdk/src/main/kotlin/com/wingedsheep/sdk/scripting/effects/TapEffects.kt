package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.targets.EffectTarget
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
) : Effect {
    override val description: String = "${if (tap) "Tap" else "Untap"} ${target.description}"
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
 */
@SerialName("PhaseOut")
@Serializable
data class PhaseOutEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String = "${target.description} phases out"
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
