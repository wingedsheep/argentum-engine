package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ajani, Mentor of Heroes — Journey into Nyx #145 (canonical printing)
 * {3}{G}{W} · Legendary Planeswalker — Ajani · Loyalty 4
 *
 * +1: Distribute three +1/+1 counters among one, two, or three target creatures you control.
 * +1: Look at the top four cards of your library. You may reveal an Aura, creature, or
 * planeswalker card from among them and put it into your hand. Put the rest on the bottom of
 * your library in any order.
 * −8: You gain 100 life.
 *
 * The first +1 is Armament Corps' distribution (one to three targets, at least one counter each).
 * The second is [Patterns.Library.lookAtTopRevealMatchingToHand] with the rest ordered by the
 * controller ("in any order") rather than the pattern's random default.
 */
val AjaniMentorOfHeroes = card("Ajani, Mentor of Heroes") {
    manaCost = "{3}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Planeswalker — Ajani"
    startingLoyalty = 4
    oracleText = "+1: Distribute three +1/+1 counters among one, two, or three target creatures you control.\n" +
        "+1: Look at the top four cards of your library. You may reveal an Aura, creature, or planeswalker card " +
        "from among them and put it into your hand. Put the rest on the bottom of your library in any order.\n" +
        "−8: You gain 100 life."

    loyaltyAbility(+1) {
        target = TargetObject(filter = TargetFilter.CreatureYouControl, count = 3, minCount = 1)
        effect = Effects.DistributeCountersAmongTargets(totalCounters = 3)
    }

    loyaltyAbility(+1) {
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 4,
            filter = GameObjectFilter.Enchantment.withSubtype("Aura") or
                GameObjectFilter.Creature or
                GameObjectFilter.Planeswalker,
            prompt = "You may reveal an Aura, creature, or planeswalker card from among them and put it into your hand",
            restOrder = CardOrder.ControllerChooses,
        )
    }

    loyaltyAbility(-8) {
        effect = Effects.GainLife(100)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "145"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a30bcfa-6f95-450f-aea7-885983d7c6a5.jpg?1783939407"
    }
}
