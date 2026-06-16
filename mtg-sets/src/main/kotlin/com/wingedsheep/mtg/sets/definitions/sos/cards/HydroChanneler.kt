package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Hydro-Channeler
 * {1}{U}
 * Creature — Merfolk Wizard
 * 1/3
 *
 * {T}: Add {U}. Spend this mana only to cast an instant or sorcery spell.
 * {1}, {T}: Add one mana of any color. Spend this mana only to cast an instant or sorcery spell.
 */
val HydroChanneler = card("Hydro-Channeler") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 1
    toughness = 3
    oracleText = "{T}: Add {U}. Spend this mana only to cast an instant or sorcery spell.\n" +
        "{1}, {T}: Add one mana of any color. Spend this mana only to cast an instant or sorcery spell."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE, 1, ManaRestriction.InstantOrSorceryOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Composite(listOf(Costs.Mana("{1}"), AbilityCost.Tap))
        effect = Effects.AddManaOfChoice(restriction = ManaRestriction.InstantOrSorceryOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Mila Pesic"
        flavorText = "Only when they can defy the laws of nature can a Quandrix student claim to have mastered a spell."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/099f8400-d70a-48ef-8ff6-645eae97e072.jpg?1775937286"
    }
}
