package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SacrificeUnlessSacrificePermanentEffect

/**
 * Primeval Force
 * {2}{G}{G}{G}
 * Creature — Elemental
 * 8/8
 * When Primeval Force enters the battlefield, sacrifice it unless you sacrifice three Forests.
 */
val PrimevalForce = card("Primeval Force") {
    manaCost = "{2}{G}{G}{G}"
    typeLine = "Creature — Elemental"
    power = 8
    toughness = 8

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = SacrificeUnlessSacrificePermanentEffect(permanentType = "Forest", count = 3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "182"
        artist = "John Avon"
        flavorText = "The raw power of nature personified, demanding tribute from the land."
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1ce7fc51-0ed8-49d9-bdba-ba5a89e1e852.jpg"
    }
}
