package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Power Plant Worker
 * {5}
 * Artifact Creature — Assembly-Worker
 * 4/4
 * {3}: This creature gets +2/+2 until end of turn. If you control creatures named Mine Worker and
 * Tower Worker, put two +1/+1 counters on this creature instead. Activate only once each turn.
 *
 * The companion check is made on resolution, mirroring Mine Worker and Tower Worker.
 */
val PowerPlantWorker = card("Power Plant Worker") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Assembly-Worker"
    power = 4
    toughness = 4
    oracleText = "{3}: This creature gets +2/+2 until end of turn. If you control creatures named Mine Worker " +
        "and Tower Worker, put two +1/+1 counters on this creature instead. Activate only once each turn."

    activatedAbility {
        cost = Costs.Mana("{3}")
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.If(
            condition = Conditions.All(
                Conditions.YouControl(GameObjectFilter.Creature.named("Mine Worker")),
                Conditions.YouControl(GameObjectFilter.Creature.named("Tower Worker"))
            ),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
            otherwise = Effects.ModifyStats(2, 2, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "241"
        artist = "Aaron Miller"
        flavorText = "Mechanical workers toiled away, tirelessly fueling the impossible."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99965310-e4bd-40d9-80f5-3fd1754c61c5.jpg?1783920016"
    }
}
