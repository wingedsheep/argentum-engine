package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Forum Familiar — Murders at Karlov Manor #16
 * {W} · Creature — Cat · 1/1
 *
 * Disguise {1}{W}
 * When this creature is turned face up, return another target permanent you control to its owner's
 * hand and put a +1/+1 counter on this creature.
 *
 * The payoff is reachable only through the disguise route: a *turned face up* trigger is not an
 * enters trigger (CR 701.34), so hard-casting the Cat for {W} gets a vanilla 1/1. Cast face down for
 * {3} it is a 2/2 with ward {2}; flipping it later for {1}{W} fires the trigger and leaves a 2/2 Cat
 * (1/1 base plus the counter) having bounced one of your own permanents — the intended loop being to
 * re-buy an enters trigger rather than to lose value.
 *
 * "Another target permanent you control" excludes the Familiar itself ([TargetFilter.excludeSelf]);
 * with no other permanent on your battlefield the trigger has no legal target and is simply removed
 * from the stack (CR 608.2b), so the +1/+1 counter is not put on either — the two halves share one
 * resolution, they are not independent.
 */
val ForumFamiliar = card("Forum Familiar") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat"
    power = 1
    toughness = 1
    oracleText = "Disguise {1}{W} (You may cast this card face down for {3} as a 2/2 creature with " +
        "ward {2}. Turn it face up any time for its disguise cost.)\n" +
        "When this creature is turned face up, return another target permanent you control to its " +
        "owner's hand and put a +1/+1 counter on this creature."
    disguise = "{1}{W}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val permanent = target(
            "another target permanent you control",
            TargetPermanent(filter = TargetFilter.Permanent.youControl().copy(excludeSelf = true)),
        )
        effect = Effects.Composite(
            Effects.ReturnToHand(permanent),
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
        )
        description = "When this creature is turned face up, return another target permanent you " +
            "control to its owner's hand and put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Ilse Gort"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b06a243d-acc8-42cd-926c-98a4cc96ab21.jpg?1783912926"

        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to."
        )
        ruling(
            "2024-02-02",
            "Turning a permanent face up or face down doesn't change whether that permanent is " +
                "tapped or untapped."
        )
    }
}
