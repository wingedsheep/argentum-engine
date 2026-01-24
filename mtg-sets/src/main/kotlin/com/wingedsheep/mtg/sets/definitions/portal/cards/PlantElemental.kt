package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SacrificeUnlessSacrificePermanentEffect

/**
 * Plant Elemental
 * {1}{G}
 * Creature — Plant Elemental
 * 3/4
 * When Plant Elemental enters the battlefield, sacrifice it unless you sacrifice a Forest.
 */
val PlantElemental = card("Plant Elemental") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Plant Elemental"
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = SacrificeUnlessSacrificePermanentEffect(permanentType = "Forest")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Donato Giancola"
        flavorText = "Rooted in the forest's magic, it draws life from the land itself."
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3aa60ba-3741-4e43-8b90-c84e63bcf7c4.jpg"
    }
}
