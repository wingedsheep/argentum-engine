package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zof Consumption {4}{B}{B} // Zof Bloodbog — Zendikar Rising #132
 * Sorcery // Land · Modal double-faced card
 *
 * Front — Zof Consumption: "Each opponent loses 4 life and you gain 4 life."
 * Back — Zof Bloodbog: "This land enters tapped. {T}: Add {B}."
 *
 * The gain is a flat 4 regardless of how many opponents there are or how much life they lost
 * (per the Two-Headed Giant ruling), so it is a loss-then-gain composite rather than
 * `Effects.DrainLife`, which gains the life actually lost.
 */
private val ZofConsumptionFront = card("Zof Consumption") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each opponent loses 4 life and you gain 4 life."

    spell {
        effect = Effects.LoseLife(4, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(4)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Izzy"
        flavorText = "\"Annihilation surrounds us. Our call is to embrace its ceaseless hunger.\"\n—Voshk, oblivion priest"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98496d5b-1519-4f0c-8b46-0a43be643dfb.jpg?1783929369"
        ruling("2020-09-25", "In a Two-Headed Giant game, Zof Consumption causes the opposing team to lose 8 life and you to gain 4 life.")
    }
}

private val ZofBloodbogBack = card("Zof Bloodbog") {
    typeLine = "Land"
    colorIdentity = "B"
    oracleText = "This land enters tapped.\n{T}: Add {B}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Izzy"
        flavorText = "\"Look at this place. What more evidence do you require that existence is bleak and meaningless?\"\n—Voshk, oblivion priest"
        imageUri = "https://cards.scryfall.io/normal/back/9/8/98496d5b-1519-4f0c-8b46-0a43be643dfb.jpg?1783929369"
    }
}

val ZofConsumption: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = ZofConsumptionFront,
    backFace = ZofBloodbogBack,
)
