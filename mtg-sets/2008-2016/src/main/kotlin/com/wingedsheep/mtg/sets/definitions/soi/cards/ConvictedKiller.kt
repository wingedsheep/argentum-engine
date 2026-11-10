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

private val ConvictedKillerFront = card("Convicted Killer") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Werewolf"
    power = 2
    toughness = 2
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
        collectorNumber = "149"
        artist = "Deruchenko Alexander"
        flavorText = "None are more dangerous than those who have already been sentenced to death."
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f43f5f02-1b29-4e75-a6af-378dcc88c3df.jpg?1783937763"
    }
}

private val BrandedHowler = card("Branded Howler") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 4
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
        collectorNumber = "149"
        artist = "Deruchenko Alexander"
        flavorText = "It is said that one of the most notorious werewolf howlpacks, the Vildin, only calls " +
            "those whose hands are already stained with blood."
        imageUri = "https://cards.scryfall.io/normal/back/f/4/f43f5f02-1b29-4e75-a6af-378dcc88c3df.jpg?1783937763"
    }
}

val ConvictedKiller: CardDefinition = CardDefinition.doubleFacedCreature(ConvictedKillerFront, BrandedHowler)
