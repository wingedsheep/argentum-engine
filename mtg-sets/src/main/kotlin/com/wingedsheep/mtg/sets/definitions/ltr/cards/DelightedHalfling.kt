package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Delighted Halfling
 * {G}
 * Creature — Halfling Citizen
 * 1/2
 *
 * {T}: Add {C}.
 * {T}: Add one mana of any color. Spend this mana only to cast a legendary spell, and that spell
 * can't be countered.
 *
 * Composes `ManaRestriction.LegendarySpellsOnly` (added with Great Hall of the Citadel) with the
 * existing `ManaSpellRider.MakesSpellUncounterable` on a one-mana any-color ability.
 */
val DelightedHalfling = card("Delighted Halfling") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Halfling Citizen"
    power = 1
    toughness = 2
    oracleText = "{T}: Add {C}.\n" +
        "{T}: Add one mana of any color. Spend this mana only to cast a legendary spell, and that " +
        "spell can't be countered."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = AddManaOfChoiceEffect(
            colorSet = ManaColorSet.AnyColor,
            amount = DynamicAmount.Fixed(1),
            restriction = ManaRestriction.LegendarySpellsOnly,
            riders = setOf(ManaSpellRider.MakesSpellUncounterable)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "Inka Schulz"
        flavorText = "There were toys the Hobbit-children had never seen before, all beautiful, and some obviously magical."
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71384418-173a-4f77-adab-56e52fa23692.jpg?1686969281"
    }
}
