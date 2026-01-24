package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.UntapAllCreaturesYouControlEffect

/**
 * Mobilize
 * {G}
 * Sorcery
 * Untap all creatures you control.
 */
val Mobilize = card("Mobilize") {
    manaCost = "{G}"
    typeLine = "Sorcery"

    spell {
        effect = UntapAllCreaturesYouControlEffect
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "172"
        artist = "Rebecca Guay"
        flavorText = "Rise, warriors! The battle awaits!"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a20c68d-f6b8-415f-9d62-ce0dc5aea69c.jpg"
    }
}
