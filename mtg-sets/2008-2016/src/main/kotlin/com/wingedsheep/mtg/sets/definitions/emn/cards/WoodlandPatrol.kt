package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Woodland Patrol (Eldritch Moon #180)
 * {2}{G}
 * Creature — Human Scout
 * 3/2
 *
 * Vigilance
 */
val WoodlandPatrol = card("Woodland Patrol") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Scout"
    power = 3
    toughness = 2
    oracleText = "Vigilance"

    keywords(Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "180"
        artist = "James Ryman"
        flavorText = "Each had seen friends and family taken by the corruption, so each carried an axe at the " +
            "other's request."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a134bd1-3c5a-467e-bad1-65548b33faa5.jpg"
    }
}
