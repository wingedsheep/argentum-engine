package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shatterskull Smashing {X}{R}{R} // Shatterskull, the Hammer Pass — Zendikar Rising #161
 * Sorcery // Land · Modal double-faced card
 *
 * Front — Shatterskull Smashing: "Shatterskull Smashing deals X damage divided as you choose among
 * up to two target creatures and/or planeswalkers. If X is 6 or more, Shatterskull Smashing deals
 * twice X damage divided as you choose among them instead."
 * Back — Shatterskull, the Hammer Pass: "As this land enters, you may pay 3 life. If you don't, it
 * enters tapped. {T}: Add {R}."
 *
 * The "instead" is one division, not two: per the ruling you divide twice X (never X and then
 * double it), so the total is a single amount that reads the announced X. X is announced before
 * targets and the division (CR 601.2b, 601.2d), which is why the cast offer prices the total per X
 * (`LegalAction.totalDamageByX`) and the validator checks the split against the announced X.
 */
private val ShatterskullSmashingFront = card("Shatterskull Smashing") {
    manaCost = "{X}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Shatterskull Smashing deals X damage divided as you choose among up to two target " +
        "creatures and/or planeswalkers. If X is 6 or more, Shatterskull Smashing deals twice X damage " +
        "divided as you choose among them instead."

    spell {
        targets(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker), count = 2, optional = true)
        effect = Effects.DividedDamage(
            minTargets = 0,
            maxTargets = 2,
            dynamicTotal = DynamicAmounts.conditional(
                Conditions.CompareAmounts(DynamicAmounts.xValue(), ComparisonOperator.GTE, 6),
                ifTrue = DynamicAmounts.xValue() * 2,
                ifFalse = DynamicAmounts.xValue(),
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "161"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc7239ea-f8aa-4a6f-87bd-c35359635673.jpg?1783929355"
        ruling("2020-09-25", "You divide the damage as you cast Shatterskull Smashing, not as it resolves. If you choose two targets, each target must be assigned at least 1 damage.")
        ruling("2020-09-25", "If X is 6 or more, you divide twice X damage; you don't assign X damage and then double that damage.")
        ruling("2020-09-25", "If one of the targets becomes illegal for Shatterskull Smashing, the original division of damage still applies to the still-legal target, but the damage that would have been dealt to the illegal target isn't dealt at all.")
    }
}

private val ShatterskullTheHammerPassBack = card("Shatterskull, the Hammer Pass") {
    typeLine = "Land"
    colorIdentity = "R"
    oracleText = "As this land enters, you may pay 3 life. If you don't, it enters tapped.\n{T}: Add {R}."

    replacementEffect(EntersTapped(payLifeCost = 3))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "161"
        artist = "Adam Paquette"
        flavorText = "\"The safest way across the Skyfangs is to fly. Shatterskull Pass is a pretty distant second.\"\n—Samila, Murasa Expeditionary House"
        imageUri = "https://cards.scryfall.io/normal/back/b/c/bc7239ea-f8aa-4a6f-87bd-c35359635673.jpg?1783929355"
    }
}

val ShatterskullSmashing: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = ShatterskullSmashingFront,
    backFace = ShatterskullTheHammerPassBack,
)
