package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Footfall Crater
 * {R}
 * Enchantment — Aura
 * Enchant land
 * Enchanted land has "{T}: Target creature gains trample and haste until end of turn."
 * Cycling {1} ({1}, Discard this card: Draw a card.)
 */
val FootfallCrater = card("Footfall Crater") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "Enchanted land has \"{T}: Target creature gains trample and haste until end of turn.\"\n" +
        "Cycling {1} ({1}, Discard this card: Draw a card.)"

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Tap,
                effect = Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.BoundVariable("target")) then
                    Effects.GrantKeyword(Keyword.HASTE, EffectTarget.BoundVariable("target")),
                targetRequirements = listOf(TargetObject(filter = TargetFilter.Creature, id = "target"))
            )
        )
    }

    keywordAbility(KeywordAbility.cycling("{1}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Ravenna Tran"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1b75aac-54e3-4181-bac5-5a142757ec05.jpg?1783931050"
    }
}
