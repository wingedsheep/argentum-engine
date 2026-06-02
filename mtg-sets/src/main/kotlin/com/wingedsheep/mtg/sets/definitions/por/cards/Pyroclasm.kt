package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.Effects

/**
 * Pyroclasm
 * {1}{R}
 * Sorcery
 * Pyroclasm deals 2 damage to each creature.
 */
val Pyroclasm = card("Pyroclasm") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"

    spell {
        effect = Effects.ForEachInGroup(GroupFilter.AllCreatures, DealDamageEffect(2, EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "John Avon"
        flavorText = "A wave of fire sweeps across the land."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de214247-e5e3-4d8f-935a-797218416be1.jpg"
    }
}
