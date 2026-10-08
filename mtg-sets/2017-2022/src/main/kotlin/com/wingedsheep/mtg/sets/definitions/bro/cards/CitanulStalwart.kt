package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Citanul Stalwart
 * {G}
 * Creature — Elf Druid Soldier
 * 1/1
 * {T}, Tap an untapped artifact or creature you control: Add one mana of any color.
 */
val CitanulStalwart = card("Citanul Stalwart") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid Soldier"
    power = 1
    toughness = 1
    oracleText = "{T}, Tap an untapped artifact or creature you control: Add one mana of any color."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapPermanents(
                count = 1,
                filter = GameObjectFilter.Artifact or GameObjectFilter.Creature,
            )
        )
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}, Tap an untapped artifact or creature you control: Add one mana of any color."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Alexandr Leskinen"
        flavorText = "The endless grind of the Brothers' War had fouled Terisiare's land and blackened its skies. Argoth's defenders were determined to protect their isle from the same fate."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a842a945-21d9-432c-b970-6da65b16f309.jpg?1783920048"
    }
}
