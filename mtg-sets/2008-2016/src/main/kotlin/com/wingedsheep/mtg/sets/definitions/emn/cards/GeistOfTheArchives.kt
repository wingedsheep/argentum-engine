package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Geist of the Archives (Eldritch Moon #62)
 * {2}{U}
 * Creature — Spirit
 * 0/4
 *
 * Defender
 * At the beginning of your upkeep, scry 1. (Look at the top card of your library. You may put
 * that card on the bottom.)
 */
val GeistOfTheArchives = card("Geist of the Archives") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 0
    toughness = 4
    oracleText = "Defender\n" +
        "At the beginning of your upkeep, scry 1. (Look at the top card of your library. You may put " +
        "that card on the bottom.)"

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Lake Hurwitz"
        flavorText = "After reading the final words, it turns back to the first page and begins again."
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44d4fcbc-15b2-4370-a267-bc2c63ba2aa5.jpg"
    }
}
