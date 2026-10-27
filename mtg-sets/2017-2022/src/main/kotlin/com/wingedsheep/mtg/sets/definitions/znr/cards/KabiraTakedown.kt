package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Kabira Takedown {1}{W} // Kabira Plateau — Zendikar Rising #19
 * Instant // Land · Modal double-faced card
 *
 * Front — Kabira Takedown: "Kabira Takedown deals damage equal to the number of creatures you
 * control to target creature or planeswalker."
 * Back — Kabira Plateau: "This land enters tapped. {T}: Add {W}."
 *
 * The creature count is read on resolution.
 */
private val KabiraTakedownFront = card("Kabira Takedown") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Kabira Takedown deals damage equal to the number of creatures you control to " +
        "target creature or planeswalker."

    spell {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(DynamicAmounts.creaturesYouControl(), victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Randy Vargas"
        flavorText = "\"Down below's where things go to die.\"\n—Kreq of Sunder Bay"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/366e9845-019d-47cc-adb8-8fbbaad35b6d.jpg?1783929423"
    }
}

private val KabiraPlateauBack = card("Kabira Plateau") {
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
        collectorNumber = "19"
        artist = "Randy Vargas"
        flavorText = "\"Wise adventurers—and clever beasties—stick to the higher ground.\"\n—Kreq of Sunder Bay"
        imageUri = "https://cards.scryfall.io/normal/back/3/6/366e9845-019d-47cc-adb8-8fbbaad35b6d.jpg?1783929423"
    }
}

val KabiraTakedown: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = KabiraTakedownFront,
    backFace = KabiraPlateauBack,
)
