package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Akoum Warrior {5}{R} // Akoum Teeth — Zendikar Rising #134
 * Creature — Minotaur Warrior 4/5 // Land · Modal double-faced card
 *
 * Front — Akoum Warrior: "Trample"
 * Back — Akoum Teeth: "This land enters tapped. {T}: Add {R}."
 */
private val AkoumWarriorFront = card("Akoum Warrior") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Warrior"
    oracleText = "Trample"
    power = 4
    toughness = 5

    keywords(Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Karl Kopinski"
        flavorText = "\"I'm not cruel, but I have no patience for your mistakes.\"\n—Tyrux, expedition warrior"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8ed0335-daa6-4dbe-a94d-4d56c8cfd093.jpg?1783929367"
    }
}

private val AkoumTeethBack = card("Akoum Teeth") {
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
        collectorNumber = "134"
        artist = "Karl Kopinski"
        flavorText = "\"Rock. Lava. Gravity. They're not cruel, but they're not forgiving either.\"\n—Tyrux, expedition warrior"
        imageUri = "https://cards.scryfall.io/normal/back/d/8/d8ed0335-daa6-4dbe-a94d-4d56c8cfd093.jpg?1783929367"
    }
}

val AkoumWarrior: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = AkoumWarriorFront,
    backFace = AkoumTeethBack,
)
