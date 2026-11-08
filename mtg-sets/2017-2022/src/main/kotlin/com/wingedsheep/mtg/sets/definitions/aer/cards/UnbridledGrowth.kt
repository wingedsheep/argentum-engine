package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Unbridled Growth — Aether Revolt #126
 * {G} · Enchantment — Aura
 *
 * Enchant land
 * Enchanted land has "{T}: Add one mana of any color."
 * Sacrifice this Aura: Draw a card.
 */
val UnbridledGrowth = card("Unbridled Growth") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "Enchanted land has \"{T}: Add one mana of any color.\"\n" +
        "Sacrifice this Aura: Draw a card."

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddAnyColorMana(1),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            )
        )
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.DrawCards(1)
        description = "Sacrifice this Aura: Draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Ryan Pancoast"
        flavorText = "\"If the Consulate has abandoned restraint, so too shall the wilds.\"\n—Surash, keeper of the Cowl"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/393fc419-a6ce-447d-9994-744cf41f9a27.jpg?1783936737"
    }
}
