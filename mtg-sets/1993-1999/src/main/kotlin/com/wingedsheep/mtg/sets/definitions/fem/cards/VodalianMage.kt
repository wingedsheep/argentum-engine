package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetSpell

/**
 * Vodalian Mage
 * {2}{U}
 * Creature — Merfolk Wizard
 * 1/1
 * {U}, {T}: Counter target spell unless its controller pays {1}.
 */
val VodalianMage = card("Vodalian Mage") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    oracleText = "{U}, {T}: Counter target spell unless its controller pays {1}."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.Tap)
        target = TargetSpell()
        effect = Effects.CounterUnlessPays("{1}")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30a"
        artist = "Susan Van Camp"
        flavorText = "\"Vodalian Mages are remarkable. Their merchants bring them arcane lore and devices from across the seas.\"\n—Lydia Wynforth, Mayor of Trokair"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c107e82b-134a-4f2b-98c2-6537fae6a50d.jpg?1783947908"
    }
}
