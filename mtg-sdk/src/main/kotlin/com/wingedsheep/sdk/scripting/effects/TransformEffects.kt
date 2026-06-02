package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Transform Effects
// =============================================================================

/**
 * Transform a double-faced permanent.
 * Toggles between front and back face.
 * "Transform this creature"
 */
@SerialName("Transform")
@Serializable
data class TransformEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String = "Transform ${target.description}"
}

/**
 * Return the source of a Craft activated ability (CR 702.167a) from exile to the battlefield
 * transformed under its owner's control.
 *
 * The source has just been exiled by the Craft cost (paired with
 * [com.wingedsheep.sdk.scripting.AbilityCost.Craft]); this effect is what the activated
 * ability resolves to. It zone-moves the source from EXILE → BATTLEFIELD, sets its
 * [com.wingedsheep.engine.state.components.identity.DoubleFacedComponent] to its back face,
 * and re-attaches the `CraftedFromExiledComponent` recording the exiled materials so the
 * back face's "exiled cards used to craft it" CDA (CR 702.167c) can read them.
 *
 * Not a generic "return-and-transform" — only valid for the Craft pattern, where the source
 * being in exile and the materials being linked to it are guaranteed by the paired cost.
 */
@SerialName("ReturnSelfFromExileTransformed")
@Serializable
data object ReturnSelfFromExileTransformedEffect : Effect {
    override val description: String =
        "Return this card to the battlefield transformed under its owner's control"
}
