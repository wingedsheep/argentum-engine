package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vexing Radgull — the rad check reads the damaged player's counters at resolution: with none,
 * that player gets two; with any, the Radgull's controller proliferates instead (choosing any
 * permanents and/or players with counters, the damaged player included or not).
 */
val VexingRadgull = card("Vexing Radgull") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Bird Mutant"
    power = 1
    toughness = 2
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player, that player gets two rad counters if they don't have any rad counters. Otherwise, proliferate. " +
        "(Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.If(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.playerCounterCount(CounterType.RAD, Player.TriggeringPlayer),
                ComparisonOperator.EQ,
                0
            ),
            then = Effects.AddCounters(CounterType.RAD, 2, EffectTarget.PlayerRef(Player.TriggeringPlayer)),
            otherwise = Effects.Proliferate()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Josu Solano"
        flavorText = "Fighting one is a terrible experience."
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91580910-fe0e-4cd9-9e73-821af23abfe7.jpg?1783912410"
    }
}
