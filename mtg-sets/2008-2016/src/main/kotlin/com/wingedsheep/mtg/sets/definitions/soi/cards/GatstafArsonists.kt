package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
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

private val GatstafArsonistsFront = card("Gatstaf Arsonists") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Werewolf"
    power = 5
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
        collectorNumber = "158"
        artist = "Greg Staples"
        flavorText = "The glow in their eyes reveals more than anger or madness."
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6ab67098-d3d5-4799-ae03-c734244f370e.jpg?1783937760"
    }
}

private val GatstafRavagers = card("Gatstaf Ravagers") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Creature — Werewolf"
    power = 6
    toughness = 5
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "At the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.mostSpellsCastLastTurnByAPlayer(), ComparisonOperator.GTE, 2
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/back/6/a/6ab67098-d3d5-4799-ae03-c734244f370e.jpg?1783937760"
    }
}

val GatstafArsonists: CardDefinition = CardDefinition.doubleFacedCreature(GatstafArsonistsFront, GatstafRavagers)
