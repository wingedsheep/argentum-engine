package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Asgardian Citadel
 * Land
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {R} or {W}.
 *
 * Implementation note: the standard "gain land" dual cycle — an [EntersTapped] replacement
 * effect, an enters-the-battlefield trigger gaining 1 life, and two separate single-colour
 * mana abilities (a mana ability may only produce one of the two colours per activation).
 */
val AsgardianCitadel = card("Asgardian Citadel") {
    manaCost = ""
    colorIdentity = "WR"
    typeLine = "Land"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {R} or {W}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "258"
        artist = "Paulius Daščioras"
        flavorText = "\"Asgard remains Asgard, be it in space, a mystic realm, or floating above Broxton, Oklahoma.\"\n—Thor"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5f88c3d-b17b-46aa-a573-449350f95d46.jpg?1783902886"
    }
}
