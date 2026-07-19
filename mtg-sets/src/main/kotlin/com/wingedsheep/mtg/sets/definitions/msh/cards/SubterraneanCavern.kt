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
 * Subterranean Cavern
 * Land
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {B} or {G}.
 *
 * Implementation note: the standard "gain land" dual cycle — an [EntersTapped] replacement
 * effect, an enters-the-battlefield trigger gaining 1 life, and two separate single-colour
 * mana abilities (a mana ability may only produce one of the two colours per activation).
 */
val SubterraneanCavern = card("Subterranean Cavern") {
    manaCost = ""
    colorIdentity = "BG"
    typeLine = "Land"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {B} or {G}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "273"
        artist = "JB Casacop"
        flavorText = "\"Down here, I am no longer alone. I needed a kingdom, and they needed a king.\"\n—Mole Man, Harvey Elder"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/038bf500-e23a-4d38-9312-db1909e20353.jpg?1783902881"
    }
}
