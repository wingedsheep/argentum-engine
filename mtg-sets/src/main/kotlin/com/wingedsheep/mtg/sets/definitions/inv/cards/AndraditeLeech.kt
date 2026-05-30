package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Andradite Leech
 * {2}{B}
 * Creature — Leech
 * 2/2
 * Black spells you cast cost {B} more to cast.
 * {B}: This creature gets +1/+1 until end of turn.
 */
val AndraditeLeech = card("Andradite Leech") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Leech"
    power = 2
    toughness = 2
    oracleText = "Black spells you cast cost {B} more to cast.\n{B}: This creature gets +1/+1 until end of turn."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withColor(Color.BLACK)),
            modification = CostModification.IncreaseColored("{B}"),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.ModifyStats(power = 1, toughness = 1, target = EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Wayne England"
        flavorText = "\"Older specimens are completely encrusted with gems, which serve as both armor and weapons.\"\n—Tolarian research notes"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6da0d4f3-9216-406c-8f3e-b9bb0a11dc75.jpg?1562916972"
    }
}
