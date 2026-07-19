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
 * A.I.M. Labs
 * Land
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {U} or {B}.
 *
 * Implementation note: the standard "gain land" dual cycle — an [EntersTapped] replacement
 * effect, an enters-the-battlefield trigger gaining 1 life, and two separate single-colour
 * mana abilities (a mana ability may only produce one of the two colours per activation).
 */
val AimLabs = card("A.I.M. Labs") {
    manaCost = ""
    colorIdentity = "UB"
    typeLine = "Land"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {U} or {B}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "257"
        artist = "Lixin Yin"
        flavorText = "\"Once I was a mere human guinea pig for A.I.M. scientists. But they did their job too well, and now I am their master!\"\n—M.O.D.O.K."
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca24cd9f-fd9f-4ea0-8b9c-ecf98db219a6.jpg?1783902887"
    }
}
