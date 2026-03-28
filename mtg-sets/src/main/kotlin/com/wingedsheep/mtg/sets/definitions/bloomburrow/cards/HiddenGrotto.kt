package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect

/**
 * Hidden Grotto
 * Land
 *
 * When this land enters, surveil 1.
 * {T}: Add {C}.
 * {1}, {T}: Add one mana of any color.
 */
val HiddenGrotto = card("Hidden Grotto") {
    typeLine = "Land"
    oracleText = "When this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)\n{T}: Add {C}.\n{1}, {T}: Add one mana of any color."

    // ETB: surveil 1
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(1)
    }

    // {T}: Add {C}
    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // {1}, {T}: Add one mana of any color
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "254"
        artist = "Fiona Hsieh"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4ba8f2e7-8357-4862-97dc-1942d066023a.jpg?1721427326"
    }
}
