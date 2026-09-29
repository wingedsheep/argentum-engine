package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Omen Hawker
 * {U}
 * Creature — Octopus Advisor
 * 1/1
 *
 * {T}: Add {C}{U}. Spend this mana only to activate abilities.
 *
 * Both halves of the mana carry [ManaRestriction.AbilityActivationOnly] (the same restriction as
 * The Enigma Jewel): any ability activation of any source, never a spell.
 */
val OmenHawker = card("Omen Hawker") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Octopus Advisor"
    power = 1
    toughness = 1
    oracleText = "{T}: Add {C}{U}. Spend this mana only to activate abilities."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1, restriction = ManaRestriction.AbilityActivationOnly) then
            Effects.AddMana(Color.BLUE, 1, restriction = ManaRestriction.AbilityActivationOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {C}{U}. Spend this mana only to activate abilities."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "Josh Hass"
        flavorText = "After years of fabricating visions for naive customers of the Obscura, Stafano was flummoxed by the nightmarish scenes that played out unbidden in his mind's eye."
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0749c3c-e3f7-4c96-8c5b-4fd2401544c4.jpg?1783917029"
    }
}
