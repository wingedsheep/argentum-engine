package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Mourning
 * {1}{B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets -2/-0.
 * {B}: Return this Aura to its owner's hand.
 */
val Mourning = card("Mourning") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets -2/-0.\n" +
        "{B}: Return this Aura to its owner's hand."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(-2, 0)
    }

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        description = "{B}: Return this Aura to its owner's hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Terese Nielsen"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/4649d881-709f-4ed0-91de-744d232a82f5.jpg?1562909240"
    }
}
