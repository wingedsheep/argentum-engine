package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Solitary Hunter // One of the Pack
 */
private val SolitaryHunterFront = card("Solitary Hunter") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior Werewolf"
    power = 3
    toughness = 4
    oracleText = "At the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.EQ, 0
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "229"
        artist = "Craig J Spearing"
        flavorText = "\"Making a living in Kessig is hard, but I've learned some tricks of the trade.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c200ef1-ea08-48d4-ab50-77940a0e08c3.jpg?1783937726"
    }
}

private val OneOfThePack = card("One of the Pack") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G"
    typeLine = "Creature — Werewolf"
    power = 5
    toughness = 6
    oracleText = "At the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.mostSpellsCastLastTurnByAPlayer(), ComparisonOperator.GTE, 2
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "229"
        artist = "Craig J Spearing"
        flavorText = "When the howl beckons, he cannot resist the urge to join the others."
        imageUri = "https://cards.scryfall.io/normal/back/6/c/6c200ef1-ea08-48d4-ab50-77940a0e08c3.jpg?1783937726"
    }
}

val SolitaryHunter: CardDefinition =
    CardDefinition.doubleFacedCreature(SolitaryHunterFront, OneOfThePack)
