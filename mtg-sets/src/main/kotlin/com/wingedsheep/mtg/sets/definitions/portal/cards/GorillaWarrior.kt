package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gorilla Warrior
 * {2}{G}
 * Creature — Ape Warrior
 * 3/2
 */
val GorillaWarrior = card("Gorilla Warrior") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Ape Warrior"
    power = 3
    toughness = 2

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Paolo Parente"
        flavorText = "The great apes fight with fury and purpose."
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38f9c3f3-0d4d-4eec-bd14-9be3233178dc.jpg"
    }
}
