package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Council of Advisors
 * {2}{U}
 * Creature — Human Advisor
 * 1/1
 * When this creature enters, draw a card.
 */
val CouncilOfAdvisors = card("Council of Advisors") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Advisor"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, draw a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Liu Shangying"
        flavorText = "Sun Quan's long and successful rule in the years 199 to 251 was due to his ability to choose and foster talented advisors and generals."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c59f45b-46fa-4494-9b25-cf9d3e462539.jpg"
    }
}
