package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Extinguish All Hope
 * {4}{B}{B}
 * Sorcery
 *
 * Destroy all nonenchantment creatures.
 *
 * [Effects.DestroyAll] over creatures that aren't enchantments — Coeurl's
 * `Creature and Nonenchantment` filter, so enchantment creatures (and bestowed or animated
 * enchantments) survive the sweep.
 */
val ExtinguishAllHope = card("Extinguish All Hope") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy all nonenchantment creatures."

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.Creature and GameObjectFilter.Nonenchantment)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "Chase Stone"
        flavorText = "Phenax offers the hope of life, while Pharika grants the solace of death. Between them stands Erebos, who promises nothing but eternity."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/6895024f-a04b-46cf-b020-df4487d0c758.jpg?1783939436"
    }
}
