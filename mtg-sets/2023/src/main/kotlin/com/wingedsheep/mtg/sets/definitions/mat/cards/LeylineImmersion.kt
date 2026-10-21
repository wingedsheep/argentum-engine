package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Leyline Immersion — March of the Machine: The Aftermath #21
 * {3}{G} · Enchantment — Aura
 *
 * Enchant legendary creature
 * Enchanted creature has ward {2} and "{T}: Add five mana in any combination of colors. Spend this
 * mana only to cast spells."
 *
 * Ward is [GrantWard] onto the enchanted creature; the mana ability is New Horizons' granted
 * [GrantActivatedAbility] shape with Gwenna's per-pip [Effects.AddManaInAnyCombination] and the
 * plain [ManaRestriction.SpellsOnly] restriction.
 */
val LeylineImmersion = card("Leyline Immersion") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant legendary creature\n" +
        "Enchanted creature has ward {2} and \"{T}: Add five mana in any combination of colors. " +
        "Spend this mana only to cast spells.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature.legendary())

    staticAbility {
        ability = GrantWard(WardCost.Mana("{2}"))
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddManaInAnyCombination(
                    amount = 5,
                    restriction = ManaRestriction.SpellsOnly,
                ),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Wisnu Tan"
        flavorText = "When Nissa tapped into Zhalfir's leylines, she was shocked to feel not a piece " +
            "of Dominaria, but a plane all its own."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/197f5adc-e14b-4c84-896f-ef5f01f7ff57.jpg?1783916521"
    }
}
