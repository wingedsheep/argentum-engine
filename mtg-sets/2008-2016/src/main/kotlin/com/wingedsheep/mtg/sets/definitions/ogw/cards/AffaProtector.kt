package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Affa Protector
 * {2}{W}
 * Creature — Human Soldier Ally
 * 1/4
 * Vigilance
 */
val AffaProtector = card("Affa Protector") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier Ally"
    power = 1
    toughness = 4
    oracleText = "Vigilance"

    keywords(Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Izzy"
        flavorText = "\"Through all the terrors we face, I hold on to the memory of the world before the Eldrazi came. I'd like to see that world again.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e54ac79-fbaa-4385-869b-aaf31e33a11f.jpg"
    }
}
