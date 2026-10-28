package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Tangled Florahedron {1}{G} // Tangled Vale — Zendikar Rising #211
 * Creature — Elemental 1/1 // Land · Modal double-faced card
 *
 * Front — Tangled Florahedron: "{T}: Add {G}."
 * Back — Tangled Vale: "This land enters tapped. {T}: Add {G}."
 */
private val TangledFlorahedronFront = card("Tangled Florahedron") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    oracleText = "{T}: Add {G}."
    power = 1
    toughness = 1

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "Randy Vargas"
        flavorText = "\"As Bala Ged regrows, new forms of life blossom with unfettered wonder.\"\n—Doral, Bala Ged guide"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/235d1ffc-72aa-40a2-95dc-3f6a8d495061.jpg?1783929335"
    }
}

private val TangledValeBack = card("Tangled Vale") {
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
        collectorNumber = "211"
        artist = "Randy Vargas"
        flavorText = "\"We would do well to open our eyes and remember what it is to wonder.\"\n—Doral, Bala Ged guide"
        imageUri = "https://cards.scryfall.io/normal/back/2/3/235d1ffc-72aa-40a2-95dc-3f6a8d495061.jpg?1783929335"
    }
}

val TangledFlorahedron: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = TangledFlorahedronFront,
    backFace = TangledValeBack,
)
