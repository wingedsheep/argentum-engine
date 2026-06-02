package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Zhalfirin Void
 * Land
 * When Zhalfirin Void enters the battlefield, scry 1.
 * {T}: Add {C}.
 */
val ZhalfirinVoid = card("Zhalfirin Void") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "When Zhalfirin Void enters the battlefield, scry 1.\n{T}: Add {C}."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.scry(1)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "249"
        artist = "Chase Stone"
        flavorText = "\"The wind whispers, 'come home,' but I cannot.\" —Teferi"
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e03f41f3-7304-4b45-bc16-fa1ddcc5eff0.jpg?1562744231"
    }
}
