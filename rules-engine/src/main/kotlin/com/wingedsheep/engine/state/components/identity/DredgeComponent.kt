package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveDredge
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Intrinsic dredge abilities, retained across zones and active only in the graveyard. */
@Serializable
data class DredgeComponent(val amounts: List<Int>) : Component {
    // Compile each immutable recipe once, rather than allocate effect trees on every draw check.
    // Derived data is rebuilt from amounts when a saved game is deserialized.
    @Transient
    val replacements: List<ReplaceDrawWith> = amounts.map(::dredgeReplacement)
}

/**
 * Battlefield permanent marker: this permanent grants dredge to cards in its controller's graveyard,
 * baked from [com.wingedsheep.sdk.scripting.GraveyardCardsHaveDredge] by
 * [com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler] (The Necrobloom).
 *
 * A component rather than a card-definition lookup for the same reason as
 * [GrantsMadnessToOwnedCardsComponent]: the draw-replacement gatherer walks the battlefield with no
 * card registry in hand. Read by [com.wingedsheep.engine.replacement.DredgeReplacements].
 */
@Serializable
data class GrantsDredgeToGraveyardCardsComponent(
    val grants: List<GraveyardCardsHaveDredge>
) : Component {
    // Same compiled recipe as printed dredge, one per grant, rebuilt on deserialization.
    @Transient
    val replacements: List<ReplaceDrawWith> = grants.map { dredgeReplacement(it.amount) }
}

internal fun dredgeReplacement(amount: Int): ReplaceDrawWith = ReplaceDrawWith(
    replacementEffect = Patterns.Library.mill(amount) then Effects.ReturnToHand(EffectTarget.Self),
    optional = true
)
