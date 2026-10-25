package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kiora, Behemoth Beckoner — War of the Spark #232 (canonical printing)
 * {2}{G/U} · Legendary Planeswalker — Kiora · Starting Loyalty: 7
 *
 * Whenever a creature you control with power 4 or greater enters, draw a card.
 * −1: Untap target permanent.
 *
 * The power check reads the creature as it enters (Temur Ascendancy's trigger shape), so static
 * boosts and counters it enters with count, later pumps don't.
 */
val KioraBehemothBeckoner = card("Kiora, Behemoth Beckoner") {
    manaCost = "{2}{G/U}"
    colorIdentity = "GU"
    typeLine = "Legendary Planeswalker — Kiora"
    startingLoyalty = 7
    oracleText = "Whenever a creature you control with power 4 or greater enters, draw a card.\n" +
        "−1: Untap target permanent."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().powerAtLeast(4)).enters()
        effect = Effects.DrawCards(1)
    }

    loyaltyAbility(-1) {
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.Untap(permanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "232"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a61b1a6-a75f-47b0-b215-de9708b6d773.jpg?1783933378"
        ruling(
            "2019-05-03",
            "The entering creature must have power 4 or greater as it enters the battlefield, or " +
                "Kiora's first ability won't trigger. Static abilities that raise or lower a creature's " +
                "power are taken into account, as well as any +1/+1 counters the creature enters with. " +
                "However, you can't have a creature with power 3 or less enter the battlefield, raise " +
                "its power with a spell, an activated ability, or a triggered ability, and have Kiora's " +
                "ability trigger."
        )
        ruling(
            "2019-05-03",
            "If the entering creature's power changes to 3 or less after it has entered the " +
                "battlefield, you'll still draw a card."
        )
    }
}
