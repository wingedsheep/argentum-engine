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
    override val description: String = "${if (tap) "Tap" else "Untap"} each permanent in $collectionName"
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
