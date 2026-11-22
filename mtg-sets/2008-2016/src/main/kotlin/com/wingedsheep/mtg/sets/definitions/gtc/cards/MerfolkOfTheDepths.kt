package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Merfolk of the Depths
 * {4}{G/U}{G/U}
 * Creature — Merfolk Soldier
 * 4/2
 * Flash
 */
val MerfolkOfTheDepths = card("Merfolk of the Depths") {
    manaCost = "{4}{G/U}{G/U}"
    colorIdentity = "UG"
    typeLine = "Creature — Merfolk Soldier"
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)"
    power = 4
    toughness = 2

    keywords(Keyword.FLASH)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Scott Chou"
        flavorText = "When giant sinkholes opened across Ravnica, they revealed buried oceans and the merfolk, once thought to be extinct."
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bddb2e15-a53e-4647-a627-6c7032429fca.jpg"
    }
}
