package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Bounding Felidar
 * {5}{W}
 * Creature — Cat Beast Mount
 * 4/7
 * Whenever this creature attacks while saddled, put a +1/+1 counter on each other creature you
 * control. You gain 1 life for each of those creatures.
 * Saddle 2 (Tap any number of other creatures you control with total power 2 or more:
 * This Mount becomes saddled until end of turn. Saddle only as a sorcery.)
 */
val BoundingFelidar = card("Bounding Felidar") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Beast Mount"
    power = 4
    toughness = 7
    oracleText = "Whenever this creature attacks while saddled, put a +1/+1 counter on each other " +
        "creature you control. You gain 1 life for each of those creatures.\n" +
        "Saddle 2 (Tap any number of other creatures you control with total power 2 or more: " +
        "This Mount becomes saddled until end of turn. Saddle only as a sorcery.)"

    keywordAbility(KeywordAbility.saddle(2))

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.SourceIsSaddled
        effect = Effects.Composite(
            listOf(
                Effects.ForEachInGroup(
                    filter = GroupFilter(
                        baseFilter = GameObjectFilter.Creature.youControl(),
                        excludeSelf = true
                    ),
                    effect = AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
                ),
                // "You gain 1 life for each of those creatures" — the other creatures you control,
                // whether or not counters could be placed (CR ruling 2024-04-12).
                Effects.GainLife(
                    DynamicAmount.AggregateBattlefield(
                        player = Player.You,
                        filter = GameObjectFilter.Creature,
                        aggregation = Aggregation.COUNT,
                        excludeSelf = true
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/8925279f-c16a-43b4-b791-ce450157275b.jpg"

        ruling("2024-04-12", "In the rare case where +1/+1 counters can't be put on one or more creatures you control, you still gain 1 life for each creature you control when Bounding Felidar's first ability resolves.")
        ruling("2024-04-12", "An ability that triggers when a creature \"attacks while saddled\" will trigger only if that creature was saddled when it was declared as an attacker.")
        ruling("2024-04-12", "\"Saddled\" isn't an ability that a creature has. It's just something true about that creature. It won't stop being saddled until the turn ends or it leaves the battlefield.")
    }
}
