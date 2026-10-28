package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Kazuul's Fury {2}{R} // Kazuul's Cliffs — Zendikar Rising #146
 * Instant // Land · Modal double-faced card
 *
 * Front — Kazuul's Fury: "As an additional cost to cast this spell, sacrifice a creature.
 * Kazuul's Fury deals damage equal to the sacrificed creature's power to any target."
 * Back — Kazuul's Cliffs: "This land enters tapped. {T}: Add {R}."
 *
 * The damage reads the sacrificed creature's last-known power from the cost payment.
 */
private val KazuulsFuryFront = card("Kazuul's Fury") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\n" +
        "Kazuul's Fury deals damage equal to the sacrificed creature's power to any target."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.sacrificedPower(), t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "146"
        artist = "Karl Kopinski"
        flavorText = "\"The toll is steep, but the cliffs are steeper.\"\n—Samila, Murasa Expeditionary House"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75240bbc-adc7-48ff-9523-c79776d710d3.jpg?1783929363"
    }
}

private val KazuulsCliffsBack = card("Kazuul's Cliffs") {
    typeLine = "Land"
    colorIdentity = "R"
    oracleText = "This land enters tapped.\n{T}: Add {R}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "146"
        artist = "Karl Kopinski"
        flavorText = "\"The path up the cliffs is nothing but steep switchbacks and rickety lifts, made by " +
            "elves, maintained by humans, and guarded by ogres. But it's the best way into the heart of " +
            "Murasa.\"\n—Samila, Murasa Expeditionary House"
        imageUri = "https://cards.scryfall.io/normal/back/7/5/75240bbc-adc7-48ff-9523-c79776d710d3.jpg?1783929363"
    }
}

val KazuulsFury: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = KazuulsFuryFront,
    backFace = KazuulsCliffsBack,
)
