package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.GroupFilter

/**
 * Pyroclasm
 * {1}{R}
 * Sorcery
 * Pyroclasm deals 2 damage to each creature.
 */
val Pyroclasm = card("Pyroclasm") {
    manaCost = "{1}{R}"
    typeLine = "Sorcery"

    spell {
        effect = ForEachInGroupEffect(GroupFilter.AllCreatures, DealDamageEffect(2, EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "John Avon"
        flavorText = "A wave of fire sweeps across the land."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de214247-e5e3-4d8f-935a-797218416be1.jpg"
    }
}
