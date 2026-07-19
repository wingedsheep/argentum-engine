package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Surveillance Room
 * Land
 * When this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 * {T}: Add {C}.
 * {1}, {T}: Add one mana of any color.
 *
 * Implementation note: composes the existing surveil pattern for the enters trigger, and the
 * standard colorless / filtered any-color mana abilities. The land enters untapped, so no
 * [com.wingedsheep.sdk.scripting.EntersTapped] replacement effect here.
 */
val SurveillanceRoom = card("Surveillance Room") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "When this land enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)\n{T}: Add {C}.\n{1}, {T}: Add one mana of any color."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.surveil(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "274"
        artist = "Maxim Ruabtsev"
        flavorText = "S.H.I.E.L.D. always keeps a watchful eye on the world from the shadows."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8fb51fbb-ab7f-4484-a3c9-dc50cb6f2756.jpg?1783902881"
    }
}
