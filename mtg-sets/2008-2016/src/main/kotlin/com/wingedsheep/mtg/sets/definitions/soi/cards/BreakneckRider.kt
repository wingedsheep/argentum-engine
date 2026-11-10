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
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private val BreakneckRiderFront = card("Breakneck Rider") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Scout Werewolf"
    power = 3
    toughness = 3
    oracleText = "At the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.EQ, 0
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Victor Adame Minguez"
        flavorText = "Only the most skillful riders brave the Breakneck Ride through Kessig."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe9f85e8-1e4c-4478-8344-71fae357b694.jpg?1783937764"
    }
}

/**
 * "Attacking" is a state predicate on the group filter (same shape as Windbrisk Raptor): creatures
 * get the bonus and trample while they're attacking, Neck Breaker included.
 */
private val NeckBreaker = card("Neck Breaker") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 3
    oracleText = "Attacking creatures you control get +1/+0 and have trample.\n" +
        "At the beginning of each upkeep, if a player cast two or more spells last turn, transform this creature."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.attacking().youControl())
        )
    }
    staticAbility {
        ability = GrantKeyword(
            Keyword.TRAMPLE,
            GroupFilter(GameObjectFilter.Creature.attacking().youControl())
        )
    }
    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.mostSpellsCastLastTurnByAPlayer(), ComparisonOperator.GTE, 2
        )
        effect = Effects.Transform(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Victor Adame Minguez"
        flavorText = "He can't outrun what he is."
        imageUri = "https://cards.scryfall.io/normal/back/f/e/fe9f85e8-1e4c-4478-8344-71fae357b694.jpg?1783937764"
    }
}

val BreakneckRider: CardDefinition = CardDefinition.doubleFacedCreature(BreakneckRiderFront, NeckBreaker)
