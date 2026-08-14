package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Agent Maria Hill — Marvel Super Heroes #2
 * {W} · Legendary Creature — Human Spy Hero · Uncommon · 2/1
 *
 * Whenever Agent Maria Hill becomes tapped to pay a teamwork cost, put a +1/+1 counter on her and
 * draw a card.
 *
 * The set's only *payer-side* teamwork payoff: every other teamwork card rewards the spell that was
 * cast, this one rewards a creature that was tapped to pay for it. What makes it expressible is the
 * tap *cause* on the tap event
 * ([com.wingedsheep.sdk.scripting.TapReason.TEAMWORK], read via
 * [com.wingedsheep.sdk.scripting.EventPattern.TapEvent.reason]) — tapping her to attack, to crew, or
 * for mana is the same transition performed by the same player, so nothing else on the event can
 * separate them.
 *
 * Note the trigger is on *her* becoming tapped, not on the spell being cast: a teamwork cost she is
 * not among the payers for does nothing, and one cast that taps her and two others still fires this
 * once — the binding is SELF, so only her own tap is an occurrence of the trigger event. At 2 power
 * she alone satisfies a teamwork 1 or 2 cost.
 */
val AgentMariaHill = card("Agent Maria Hill") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Spy Hero"
    oracleText = "Whenever Agent Maria Hill becomes tapped to pay a teamwork cost, put a +1/+1 " +
        "counter on her and draw a card."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.BecomesTappedForTeamwork
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "Jake Murray"
        flavorText = "\"You're going to have to trust me. You don't have the security clearance " +
            "for the truth.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e327c67-1cf1-4d82-903c-b41c8e7cf747.jpg?1783902980"
    }
}
