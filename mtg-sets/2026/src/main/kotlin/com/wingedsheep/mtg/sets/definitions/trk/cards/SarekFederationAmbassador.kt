package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sarek, Federation Ambassador
 * {1}{G}{W}{U}
 * Legendary Creature — Vulcan Advisor
 * 0/5
 * Federation — At the beginning of combat on your turn, draw a card for every five creature types
 * among non-Borg creatures you control. If you draw three or more cards this way, put two +1/+1
 * counters on each creature you control.
 *
 * "For every five" is the Federation count divided by five, rounded down. "If you draw three or
 * more cards this way" reads what actually happened, not the intended count: the growth of your
 * cards-drawn-this-turn tally across the draw (the Mr. Foxglove spelling), so a short library or a
 * replaced draw that leaves fewer than three cards drawn doesn't grant the counters.
 */
val SarekFederationAmbassador = card("Sarek, Federation Ambassador") {
    manaCost = "{1}{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Legendary Creature — Vulcan Advisor"
    oracleText = "Federation — At the beginning of combat on your turn, draw a card for every five " +
        "creature types among non-Borg creatures you control. If you draw three or more cards this way, " +
        "put two +1/+1 counters on each creature you control."
    power = 0
    toughness = 5

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.Pipeline {
            val drawnThisTurn = DynamicAmounts.cardsDrawnThisTurn(Player.You)
            val drawnBefore = storeNumber(drawnThisTurn)
            run(Effects.DrawCards(DynamicAmounts.federation() / 5))
            run(Effects.If(
                condition = Conditions.CompareAmounts(
                    drawnThisTurn - drawnBefore.amount,
                    ComparisonOperator.GTE,
                    3
                ),
                then = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.youControl()),
                    Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.IterationEntity)
                )
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "251"
        artist = "Darren Tan"
        flavorText = "\"Threats are illogical.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94ad9063-9663-49d8-b7ba-05be865bd20a.jpg?1793273885"
    }
}
