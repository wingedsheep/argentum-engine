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
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a142f369-8fdd-4dc8-b5d9-3493455cc588.jpg"
    }
}
