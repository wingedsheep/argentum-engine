package com.wingedsheep.mtg.sets.definitions.c15.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Magus of the Wheel
 * {2}{R}
 * Creature — Human Wizard
 * 3/3
 * {1}{R}, {T}, Sacrifice this creature: Each player discards their hand, then draws seven cards.
 *
 * Wheel of Fortune's two-pass shape: every player discards their hand before anyone draws.
 */
val MagusOfTheWheel = card("Magus of the Wheel") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard"
    oracleText = "{1}{R}, {T}, Sacrifice this creature: Each player discards their hand, then draws seven cards."
    power = 3
    toughness = 3

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.ForEachPlayer(Player.Each, Patterns.Hand.discardHand()) then
            Effects.ForEachPlayer(Player.Each, Effects.DrawCards(7))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Carl Frank"
        flavorText = "\"Fate is not as fixed as you might think.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfe51ad5-d786-4de3-aa39-0f5fd7e73ec6.jpg?1783938111"
        ruling("2018-03-16", "If each player has fewer than seven cards in their library as Magus of the Wheel's ability resolves, the game ends in a draw.")
    }
}
