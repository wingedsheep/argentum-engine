package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Conduit Pylons
 * Land — Desert
 *
 * When this land enters, surveil 1.
 * {T}: Add {C}.
 * {1}, {T}: Add one mana of any color.
 */
val ConduitPylons = card("Conduit Pylons") {
    typeLine = "Land — Desert"
    colorIdentity = ""
    oracleText = "When this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)\n{T}: Add {C}.\n{1}, {T}: Add one mana of any color."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.surveil(1)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddAnyColorMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "254"
        artist = "Raymond Bonilla"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5ffa48cc-b991-4d47-b7ec-cf678915c758.jpg?1712356314"
    }
}
