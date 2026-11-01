package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Commence the Endgame — War of the Spark #45 (canonical printing)
 * {4}{U}{U}
 * Instant
 * This spell can't be countered.
 * Draw two cards, then amass Zombies X, where X is the number of cards in your hand.
 *
 * X is counted on resolution, after the two draws (2019-05-03 ruling).
 */
val CommenceTheEndgame = card("Commence the Endgame") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell can't be countered.\n" +
        "Draw two cards, then amass Zombies X, where X is the number of cards in your hand. " +
        "(Put X +1/+1 counters on an Army you control. It's also a Zombie. " +
        "If you don't control an Army, create a 0/0 black Zombie Army creature token first.)"

    cantBeCountered = true

    spell {
        effect = Effects.DrawCards(2) then Effects.Amass(DynamicAmounts.cardsInYourHand(), "Zombie")
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "Noah Bradley"
        flavorText = "\"Even gods shall kneel.\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/209d70a2-c8c1-4072-ab1f-57804b55a09a.jpg?1783933467"
        ruling(
            "2019-05-03",
            "A spell or ability that counters spells can still target Commence the Endgame. When that spell or ability resolves, Commence the Endgame won't be countered, but any additional effects of that spell or ability will still happen."
        )
        ruling(
            "2019-05-03",
            "You draw two cards and amass X all while Commence the Endgame is resolving. Nothing can happen between the two, and no player may choose to take actions."
        )
    }
}
