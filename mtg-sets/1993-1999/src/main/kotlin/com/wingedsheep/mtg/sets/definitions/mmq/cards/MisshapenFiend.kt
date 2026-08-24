package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Misshapen Fiend
 * {1}{B}
 * Creature — Horror Mercenary
 * 1 / 1
 *
 * Flying
 */
val MisshapenFiend = card("Misshapen Fiend") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror Mercenary"
    oracleText = "Flying"
    power = 1
    toughness = 1
    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "147"
        artist = "Adam Rex"
        flavorText = "You'd scarcely believe they could fly until they drop out of the sky onto you."
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a43cf59e-7583-4651-968a-2a7201c69b6b.jpg"
    }
}
