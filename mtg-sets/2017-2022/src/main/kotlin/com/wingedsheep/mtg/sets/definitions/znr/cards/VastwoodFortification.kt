package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vastwood Fortification {G} // Vastwood Thicket — Zendikar Rising #216
 * Instant // Land · Modal double-faced card
 *
 * Front — Vastwood Fortification: "Put a +1/+1 counter on target creature."
 * Back — Vastwood Thicket: "This land enters tapped. {T}: Add {G}."
 */
private val VastwoodFortificationFront = card("Vastwood Fortification") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 1, target = t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"When you understand nature's ways, you're never unarmed.\"\n—Eyza, Joraga guide"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a7fd24e-84d8-405d-86e4-0571a9e23cc2.jpg?1783929332"
    }
}

private val VastwoodThicketBack = card("Vastwood Thicket") {
    typeLine = "Land"
    colorIdentity = "G"
    oracleText = "This land enters tapped.\n{T}: Add {G}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"Never underestimate the ferocity of the imperative to thrive.\"\n—Eyza, Joraga guide"
        imageUri = "https://cards.scryfall.io/normal/back/3/a/3a7fd24e-84d8-405d-86e4-0571a9e23cc2.jpg?1783929332"
    }
}

val VastwoodFortification: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = VastwoodFortificationFront,
    backFace = VastwoodThicketBack,
)
