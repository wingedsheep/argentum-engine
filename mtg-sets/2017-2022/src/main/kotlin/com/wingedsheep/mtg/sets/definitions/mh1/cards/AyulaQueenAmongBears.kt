package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Ayula, Queen Among Bears — Modern Horizons #155
 * {1}{G} · Legendary Creature — Bear · 2/2
 *
 * Whenever another Bear you control enters, choose one —
 * • Put two +1/+1 counters on target Bear.
 * • Target Bear you control fights target creature you don't control.
 *
 * A modal trigger; each mode declares its own targets, chosen when the trigger is put on the stack.
 */
val AyulaQueenAmongBears = card("Ayula, Queen Among Bears") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Bear"
    power = 2
    toughness = 2
    oracleText = "Whenever another Bear you control enters, choose one —\n" +
        "• Put two +1/+1 counters on target Bear.\n" +
        "• Target Bear you control fights target creature you don't control."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.withSubtype("Bear").youControl()).enters()
        effect = Effects.Modal(
            modes = listOf(
                mode("Put two +1/+1 counters on target Bear") {
                    val bear = target(TargetFilter(GameObjectFilter.Permanent.withSubtype("Bear")))
                    effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, bear)
                },
                mode("Target Bear you control fights target creature you don't control") {
                    val bear = target(TargetFilter(GameObjectFilter.Permanent.withSubtype("Bear").youControl()))
                    val victim = target(
                        TargetFilter(
                            GameObjectFilter.Creature.copy(
                                controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                            )
                        )
                    )
                    effect = Effects.Fight(bear, victim)
                }
            )
        )
        description = "Whenever another Bear you control enters, choose one — Put two +1/+1 counters " +
            "on target Bear; or target Bear you control fights target creature you don't control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Jesper Ejsing"
        flavorText = "Born from the oldest red cedar, nursed on the sap of the tallest spruce, coronated under the mightiest pine."
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c046dfb0-9ca3-4219-a6b0-d7503bc2fbd0.jpg?1783933100"
    }
}
