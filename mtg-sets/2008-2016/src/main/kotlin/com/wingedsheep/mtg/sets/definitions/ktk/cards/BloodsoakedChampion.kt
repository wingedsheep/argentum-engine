package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.Conditions

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodsoaked Champion
 * {B}
 * Creature — Human Warrior
 * 2/1
 * Bloodsoaked Champion can't block.
 * Raid — {1}{B}: Return Bloodsoaked Champion from your graveyard to the battlefield.
 * Activate only if you attacked this turn.
 */
val BloodsoakedChampion = card("Bloodsoaked Champion") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 1
    oracleText = "Bloodsoaked Champion can't block.\nRaid — {1}{B}: Return Bloodsoaked Champion from your graveyard to the battlefield. Activate only if you attacked this turn."

    staticAbility {
        ability = CantBlock()
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.YouAttackedThisTurn)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Aaron Miller"
        flavorText = "\"Death is merely another foe the Mardu will overcome.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34bc9bb0-5ec1-400f-89e7-b450980a3391.jpg?1562784700"
    }
}
