package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Finneas, Ace Archer
 * {G}{W}
 * Legendary Creature — Rabbit Archer
 * 2/2
 *
 * Vigilance, reach
 * Whenever Finneas attacks, put a +1/+1 counter on each other creature you control
 * that's a token or a Rabbit. Then if creatures you control have total power 10 or
 * greater, draw a card.
 */
val FinneasAceArcher = card("Finneas, Ace Archer") {
    manaCost = "{G}{W}"
    typeLine = "Legendary Creature — Rabbit Archer"
    power = 2
    toughness = 2
    oracleText = "Vigilance, reach\nWhenever Finneas attacks, put a +1/+1 counter on each other creature you control that's a token or a Rabbit. Then if creatures you control have total power 10 or greater, draw a card."

    keywords(Keyword.VIGILANCE, Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.Attacks

        // Put a +1/+1 counter on each other creature you control that's a token or a Rabbit
        // Then if creatures you control have total power 10 or greater, draw a card
        val tokenOrRabbitFilter = GameObjectFilter.Token or GameObjectFilter.Creature.withSubtype("Rabbit")
        val otherTokenOrRabbitYouControl = GroupFilter(
            baseFilter = tokenOrRabbitFilter.youControl(),
            excludeSelf = true
        )

        effect = CompositeEffect(
            listOf(
                ForEachInGroupEffect(
                    filter = otherTokenOrRabbitYouControl,
                    effect = AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
                ),
                ConditionalEffect(
                    condition = Compare(
                        left = DynamicAmount.AggregateBattlefield(
                            player = Player.You,
                            filter = GameObjectFilter.Creature,
                            aggregation = Aggregation.SUM,
                            property = CardNumericProperty.POWER
                        ),
                        operator = ComparisonOperator.GTE,
                        right = DynamicAmount.Fixed(10)
                    ),
                    effect = DrawCardsEffect(DynamicAmount.Fixed(1), EffectTarget.Controller)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dee197d-c313-4364-b52c-f83d5f579bc3.jpg?1721427047"
        ruling("2024-07-26", "Players can't take actions in between the time you put counters on creatures with Finneas's last ability and the point at which that ability checks the total power of creatures you control.")
    }
}
