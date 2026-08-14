package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

private val HinterlandLoggerFront = card("Hinterland Logger") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 2
    toughness = 1
    oracleText = "At the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.EachUpkeep
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.EQ, DynamicAmount.Fixed(0)
        )
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "210"
        artist = "Karl Kopinski"
        flavorText = "\"There's a forester lives up in the highlands. Tried to sell her one of my finest axes at a bargain, but she wasn't interested.\"\n—Old Rutstein"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14529bed-9632-4d58-8edc-c5a1359f6604.jpg?1783937734"
    }
}

private val TimberShredder = card("Timber Shredder") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G"
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 2
    oracleText = "Trample\nAt the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    keywords(Keyword.TRAMPLE)
    triggeredAbility {
        trigger = Triggers.EachUpkeep
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.GTE, DynamicAmount.Fixed(2)
        )
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "210"
        artist = "Karl Kopinski"
        flavorText = "\"She even had the audacity to criticize my wares as inferior.\"\n—Old Rutstein"
        imageUri = "https://cards.scryfall.io/normal/back/1/4/14529bed-9632-4d58-8edc-c5a1359f6604.jpg?1783937734"
    }
}

val HinterlandLogger: CardDefinition = CardDefinition.doubleFacedCreature(HinterlandLoggerFront, TimberShredder)
