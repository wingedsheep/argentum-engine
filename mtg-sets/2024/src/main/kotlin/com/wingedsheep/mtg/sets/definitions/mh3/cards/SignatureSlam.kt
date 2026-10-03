package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Signature Slam {2}{G}
 * Instant
 * Put a +1/+1 counter on target creature you control, then each modified creature you control
 * deals damage equal to its power to target creature you don't control.
 *
 * The counter lands first, so the targeted creature is always modified by the time the group is
 * gathered. "Modified" (CR 700.9) is [StatePredicate.IsModified]; each modified creature is its own
 * damage source, dealing damage equal to its projected power, read per iteration.
 */
val SignatureSlam = card("Signature Slam") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature you control, then each modified creature you control " +
        "deals damage equal to its power to target creature you don't control. " +
        "(Equipment, Auras you control, and counters are modifications.)"

    spell {
        val yours = target(TargetFilter.Creature.youControl())
        val victim = target(TargetFilter.Creature.opponentControls())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, yours) then
            Effects.ForEachInGroup(
                filter = GroupFilter(GameObjectFilter.Creature.youControl().withStatePredicate(StatePredicate.IsModified)),
                effect = Effects.DealDamage(
                    amount = DynamicAmounts.powerOf(EffectTarget.IterationEntity),
                    target = victim,
                    damageSource = EffectTarget.IterationEntity,
                ),
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "Slawomir Maniak"
        flavorText = "A lone dinosaur in gorilla territory doesn't stand a chance."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/634e05c9-96da-467a-870d-3af68da53071.jpg?1783911257"
        ruling("2024-06-07", "If the creature you control is an illegal target as Signature Slam tries to resolve but the creature you don't control is still a legal target, each modified creature you control (which may include the target creature you control if it's still on the battlefield) will still deal damage equal to its power to the creature you don't control.")
        ruling("2024-06-07", "If the creature you don't control is an illegal target as Signature Slam tries to resolve but the creature you control is a legal target, you'll still put a +1/+1 counter on the target creature you control.")
        ruling("2024-06-07", "An Aura controlled by another player does not cause a creature you control to be modified.")
        ruling("2024-06-07", "A creature with a counter on it is considered modified no matter what kind of counter it is or which player put it on that creature.")
        ruling("2024-06-07", "A creature that is equipped is considered modified no matter who controls the Equipment that's attached to it.")
    }
}
