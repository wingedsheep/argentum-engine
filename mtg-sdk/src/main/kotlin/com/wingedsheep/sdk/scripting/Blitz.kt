package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers

/**
 * The battlefield half of blitz (CR 702.152a) as pure data — "As long as this permanent's blitz cost
 * was paid, it has haste and 'When this permanent is put into a graveyard from the battlefield, draw
 * a card.'"
 *
 * A blitz card prints only `Blitz [cost]` and its reminder text, so the draw trigger lives here as one
 * synthesized [TriggeredAbility] that the engine grants to a permanent whose blitz cost was paid — the
 * same synthesize-from-the-rules shape as [Offspring.tokenCopyAbility] and [Sieges.defeatAbility].
 * Haste is supplied alongside it by the engine's projection off the same paid marker.
 */
object Blitz {

    /** The synthesized "when this permanent dies, draw a card" trigger a blitzed permanent has. */
    val diesDrawAbility: TriggeredAbility = Triggers.self.dies().let { spec ->
        TriggeredAbility(
            id = AbilityId("blitz_dies_draw"),
            trigger = spec.event,
            binding = spec.binding,
            effect = Effects.DrawCards(1),
            descriptionOverride = "Blitz — when this creature dies, draw a card.",
        )
    }
}
