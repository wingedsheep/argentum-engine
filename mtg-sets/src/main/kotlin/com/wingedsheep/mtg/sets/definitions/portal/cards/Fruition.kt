package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GainLifeForEachLandOnBattlefieldEffect

/**
 * Fruition
 * {G}
 * Sorcery
 * You gain 1 life for each Forest on the battlefield.
 */
val Fruition = card("Fruition") {
    manaCost = "{G}"
    typeLine = "Sorcery"

    spell {
        effect = GainLifeForEachLandOnBattlefieldEffect(
            landType = "Forest",
            lifePerLand = 1
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "166"
        artist = "Kev Walker"
        flavorText = "The forest gives life to those who respect it."
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04a28f42-aef5-41f1-b1f9-1b11e88e8e22.jpg"
    }
}
