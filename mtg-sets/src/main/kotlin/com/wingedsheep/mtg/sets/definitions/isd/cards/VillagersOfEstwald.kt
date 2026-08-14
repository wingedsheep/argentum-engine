package com.wingedsheep.mtg.sets.definitions.isd.cards

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

private val VillagersOfEstwaldFront = card("Villagers of Estwald") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Werewolf"
    power = 2
    toughness = 3
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
        collectorNumber = "209"
        artist = "Kev Walker"
        flavorText = "You can spot a werewolf-infested town by its lack of butcher shops."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e42a0a3d-a987-4b24-b9d4-27380a12e093.jpg?1783940912"
    }
}

private val HowlpackOfEstwald = card("Howlpack of Estwald") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G"
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 6
    oracleText = "At the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.EachUpkeep
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.GTE, DynamicAmount.Fixed(2)
        )
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "209"
        artist = "Kev Walker"
        flavorText = "Estwald's citizens don't dislike outsiders—they taste just fine."
        imageUri = "https://cards.scryfall.io/normal/back/e/4/e42a0a3d-a987-4b24-b9d4-27380a12e093.jpg?1783940912"
    }
}

val VillagersOfEstwald: CardDefinition = CardDefinition.doubleFacedCreature(VillagersOfEstwaldFront, HowlpackOfEstwald)
