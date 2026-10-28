package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Skyclave Cleric {1}{W} // Skyclave Basilica — Zendikar Rising #40
 * Creature — Kor Cleric 1/3 // Land · Modal double-faced card
 *
 * Front — Skyclave Cleric: "When this creature enters, you gain 2 life."
 * Back — Skyclave Basilica: "This land enters tapped. {T}: Add {W}."
 */
private val SkyclaveClericFront = card("Skyclave Cleric") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Cleric"
    oracleText = "When this creature enters, you gain 2 life."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(2)
        description = "When this creature enters, you gain 2 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Johannes Voss"
        flavorText = "\"Well, she can jump farther than I can.\"\n—Aelah, expedition cleric"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/014027c4-7f9d-4096-b308-ea4be574c0d4.jpg?1783929412"
    }
}

private val SkyclaveBasilicaBack = card("Skyclave Basilica") {
    typeLine = "Land"
    colorIdentity = "W"
    oracleText = "This land enters tapped.\n{T}: Add {W}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Johannes Voss"
        flavorText = "\"As points of no return go, it is quite epic.\"\n—Aelah, expedition cleric"
        imageUri = "https://cards.scryfall.io/normal/back/0/1/014027c4-7f9d-4096-b308-ea4be574c0d4.jpg?1783929412"
    }
}

val SkyclaveCleric: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = SkyclaveClericFront,
    backFace = SkyclaveBasilicaBack,
)
