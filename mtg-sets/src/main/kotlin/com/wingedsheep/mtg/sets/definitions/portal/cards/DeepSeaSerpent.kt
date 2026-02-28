package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CombatCondition

/**
 * Deep-Sea Serpent
 * {4}{U}{U}
 * Creature - Serpent
 * 5/5
 * Deep-Sea Serpent can't attack unless defending player controls an Island.
 */
val DeepSeaSerpent = card("Deep-Sea Serpent") {
    manaCost = "{4}{U}{U}"
    typeLine = "Creature — Serpent"
    power = 5
    toughness = 5

    staticAbility {
        ability = CantAttackUnless(CombatCondition.OpponentControlsLandType("Island"))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2d7f8d8-30fb-47c7-8927-646c41f0b9bc.jpg"
    }
}
