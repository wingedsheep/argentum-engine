package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ichor Shade
 * {2}{B}
 * Creature — Phyrexian Shade
 * 2/3
 * At the beginning of your end step, if an artifact or creature was put into a graveyard from
 * the battlefield this turn, put a +1/+1 counter on this creature.
 *
 * Game-wide, tokens included: the two per-player tallies summed over [Player.Each]. An artifact
 * creature is counted by both, which only matters for a count — the condition asks "any".
 */
val IchorShade = card("Ichor Shade") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Shade"
    oracleText = "At the beginning of your end step, if an artifact or creature was put into a graveyard " +
        "from the battlefield this turn, put a +1/+1 counter on this creature."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.creaturesDiedThisTurn(Player.Each) + DynamicAmounts.artifactsDiedThisTurn(Player.Each),
            ComparisonOperator.GT,
            0,
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Nicholas Gregory"
        flavorText = "As oil seeped into their funeral shrouds, Tolvada's dead rose to join the invaders' ranks."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fd86fbf-eac2-456b-b4bb-437ff9be9b58.jpg?1783917006"
    }
}
