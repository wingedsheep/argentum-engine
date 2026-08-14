package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

private val ScornedVillagerFront = card("Scorned Villager") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 1
    toughness = 1
    oracleText = "{T}: Add {G}.\nAt the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
    }
    triggeredAbility {
        trigger = Triggers.EachUpkeep
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.EQ, DynamicAmount.Fixed(0)
        )
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Cynthia Sheppard"
        flavorText = "\"My village's fear drove me into the wild . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f35e364-81d9-4888-993b-acc7a53d963c.jpg?1783940808"
    }
}

private val MoonscarredWerewolf = card("Moonscarred Werewolf") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G"
    typeLine = "Creature — Werewolf"
    power = 2
    toughness = 2
    oracleText = "Vigilance\n{T}: Add {G}{G}.\nAt the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    keywords(Keyword.VIGILANCE)
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN, 2)
        manaAbility = true
    }
    triggeredAbility {
        trigger = Triggers.EachUpkeep
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.GTE, DynamicAmount.Fixed(2)
        )
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Cynthia Sheppard"
        flavorText = "\". . . and I will bring the fury of the wild back to my village.\""
        imageUri = "https://cards.scryfall.io/normal/back/6/f/6f35e364-81d9-4888-993b-acc7a53d963c.jpg?1783940808"
    }
}

val ScornedVillager: CardDefinition = CardDefinition.doubleFacedCreature(ScornedVillagerFront, MoonscarredWerewolf)
