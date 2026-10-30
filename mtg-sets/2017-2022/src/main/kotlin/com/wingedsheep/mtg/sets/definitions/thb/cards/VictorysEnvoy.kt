package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Victory's Envoy
 * {3}{W}{W}
 * Creature — Human Cleric
 * 3/3
 * At the beginning of your upkeep, put a +1/+1 counter on each other creature you control.
 *
 * Web-Warriors' counter sweep — [Effects.ForEachInGroup] over other creatures you control — on a
 * your-upkeep trigger.
 */
val VictorysEnvoy = card("Victory's Envoy") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 3
    toughness = 3
    oracleText = "At the beginning of your upkeep, put a +1/+1 counter on each other creature you control."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
        description = "At the beginning of your upkeep, put a +1/+1 counter on each other creature you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "289"
        artist = "Sara Winters"
        flavorText = "Her paean is the clash of spears, her prayer a battle cry."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e7fa9e2-4c16-4e94-9b0e-09657303132c.jpg?1783931490"
    }
}
