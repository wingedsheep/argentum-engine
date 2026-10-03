package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Horrid Shadowspinner — Modern Horizons 3 #188.
 *
 * Lifelink
 * Whenever this creature attacks, you may draw cards equal to its power. If you do, discard that
 * many cards.
 *
 * The power is captured once, as the "may" is accepted, so the discard count stays the number
 * chosen to draw even if the power changes mid-resolution (2024-06-07 ruling).
 */
val HorridShadowspinner = card("Horrid Shadowspinner") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Horror"
    power = 2
    toughness = 3
    oracleText = "Lifelink\n" +
        "Whenever this creature attacks, you may draw cards equal to its power. If you do, discard " +
        "that many cards."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.May(
            Effects.Pipeline {
                val drawn = storeNumber(DynamicAmounts.sourcePower())
                run(Effects.DrawCards(drawn.amount))
                run(Effects.Discard(drawn.amount))
            },
            prompt = "Draw cards equal to Horrid Shadowspinner's power, then discard that many?"
        )
        description = "Whenever this creature attacks, you may draw cards equal to its power. " +
            "If you do, discard that many cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Ben Wootten"
        flavorText = "\"The daylight's done, the nightmare's spun. It crawls off the loom, and into " +
            "your room.\"\n—Kithkin children's rhyme"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62b69aaf-0aae-4f6e-a27f-0129882dfe1d.jpg?1783911250"
        ruling(
            "2024-06-07",
            "The number of cards you discard is equal to Horrid Shadowspinner's power at the time you " +
                "chose to draw cards, even if another effect changed the number of cards you drew or in the " +
                "unusual case where Horrid Shadowspinner's power has changed since you chose to draw cards."
        )
    }
}
