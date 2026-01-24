package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageToAllEffect

/**
 * Dry Spell
 * {1}{B}
 * Sorcery
 * Dry Spell deals 1 damage to each creature and each player.
 */
val DrySpell = card("Dry Spell") {
    manaCost = "{1}{B}"
    typeLine = "Sorcery"

    spell {
        effect = DealDamageToAllEffect(amount = 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5db37eb4-8c71-4d77-9a7d-fd4e09a20e0a.jpg"
    }
}
