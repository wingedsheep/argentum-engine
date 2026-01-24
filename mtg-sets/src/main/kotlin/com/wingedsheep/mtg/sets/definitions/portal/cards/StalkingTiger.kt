package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan

/**
 * Stalking Tiger
 * {3}{G}
 * Creature — Cat
 * 3/3
 * Stalking Tiger can't be blocked by more than one creature.
 */
val StalkingTiger = card("Stalking Tiger") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Cat"
    power = 3
    toughness = 3

    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "186"
        artist = "Una Fricker"
        flavorText = "Silent as a shadow, patient as stone."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2de80a15-06f8-4f46-9ad6-fb6ddc3b3346.jpg"
    }
}
