package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.BendType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Emit a "you bent" notification for the resolving effect's controller and record the [bendType] in
 * that player's per-turn bend set (CR 701.65b Airbend / 701.66b Earthbend / 702.189b Firebending).
 *
 * Pure marker, mirroring [EmitSurveiledEventEffect]: the executor appends a `BendPerformedEvent` so
 * [com.wingedsheep.sdk.dsl.Triggers.YouBend] triggers fire, and folds [bendType] into the player's
 * `BendsThisTurnComponent` so [com.wingedsheep.sdk.scripting.values.TurnTracker.DISTINCT_BENDS] can
 * back "if you've done all four this turn". It is composed into
 * [com.wingedsheep.sdk.dsl.Effects.Earthbend] / [com.wingedsheep.sdk.dsl.Effects.Airbend] and the
 * firebending attack trigger so every bend fires the trigger uniformly.
 *
 * Waterbend (CR 701.67c) is emitted engine-side when the waterbend cost is *paid*, regardless of how
 * it was paid — not through this effect, because paying a waterbend cost is a cost event, not the
 * resolution of an effect.
 *
 * Card authors should not use this directly; it is wired into the bend primitives.
 */
@SerialName("EmitBendEvent")
@Serializable
data class EmitBendEventEffect(
    val bendType: BendType
) : Effect {
    // Intentionally blank: internal pipeline marker with no player-facing text.
    override val description: String = ""
}
