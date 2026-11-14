package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Avaricious Dragon
 * {2}{R}{R}
 * Creature — Dragon
 * 4/4
 *
 * Flying
 * At the beginning of your draw step, draw an additional card.
 * At the beginning of your end step, discard your hand.
 *
 * Both triggers are the pair Sarkhan, the Dragonspeaker's emblem uses: a draw-step trigger drawing
 * one card on top of the turn-based draw, and [Patterns.Hand.discardHand] at your end step.
 */
val AvariciousDragon = card("Avaricious Dragon") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    oracleText = "Flying\n" +
        "At the beginning of your draw step, draw an additional card.\n" +
        "At the beginning of your end step, discard your hand."
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.DRAW)
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Patterns.Hand.discardHand()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "131"
        artist = "Chris Rahn"
        flavorText = "Waste follows want."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/354255d7-dc29-403b-aa90-9044cde669d0.jpg?1783938333"
    }
}
