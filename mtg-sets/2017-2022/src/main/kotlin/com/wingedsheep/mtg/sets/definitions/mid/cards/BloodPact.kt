package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Blood Pact
 * {2}{B}
 * Instant
 * Target player draws two cards and loses 2 life.
 */
val BloodPact = card("Blood Pact") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target player draws two cards and loses 2 life."

    spell {
        val player = target(Targets.Player)
        effect = Effects.DrawCards(2, player) then Effects.LoseLife(2, player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Sam White"
        flavorText = "Griselbrand's defeat did not mark the end of the Skirsdag cult. They simply turned their worship to the next demon to emerge from the shadows bearing offers of power."
        imageUri = "https://cards.scryfall.io/normal/front/6/0/607b1066-1ca0-45e1-a55c-30aed77cc8dc.jpg?1783925625"
    }
}
