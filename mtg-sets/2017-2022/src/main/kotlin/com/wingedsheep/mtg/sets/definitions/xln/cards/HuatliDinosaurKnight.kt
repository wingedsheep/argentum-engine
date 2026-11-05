package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Huatli, Dinosaur Knight
 * {4}{R}{W}
 * Legendary Planeswalker — Huatli
 * Starting Loyalty: 4
 *
 * +2: Put two +1/+1 counters on up to one target Dinosaur you control.
 * −3: Target Dinosaur you control deals damage equal to its power to target creature you don't control.
 * −7: Dinosaurs you control get +4/+4 until end of turn.
 *
 * The −3 is the Hunter's Edge fight-less bite: if either target is illegal on resolution, no damage
 * is dealt (ruling). The −7 locks in the Dinosaurs present on resolution (ForEachInGroup snapshot).
 */
val HuatliDinosaurKnight = card("Huatli, Dinosaur Knight") {
    manaCost = "{4}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Planeswalker — Huatli"
    startingLoyalty = 4
    oracleText = "+2: Put two +1/+1 counters on up to one target Dinosaur you control.\n" +
        "−3: Target Dinosaur you control deals damage equal to its power to target creature you don't control.\n" +
        "−7: Dinosaurs you control get +4/+4 until end of turn."

    loyaltyAbility(+2) {
        val dino = target(
            TargetFilter(GameObjectFilter.Permanent.withSubtype("Dinosaur").youControl()),
            optional = true
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, dino)
        description = "Put two +1/+1 counters on up to one target Dinosaur you control."
    }

    loyaltyAbility(-3) {
        val dino = target(TargetFilter(GameObjectFilter.Permanent.withSubtype("Dinosaur").youControl()))
        val theirs = target(TargetFilter.Creature.opponentControls())
        effect = Effects.DealDamage(DynamicAmounts.powerOf(dino), theirs, damageSource = dino)
        description = "Target Dinosaur you control deals damage equal to its power to target creature " +
            "you don't control."
    }

    loyaltyAbility(-7) {
        effect = Patterns.Group.modifyStatsForAll(
            power = 4,
            toughness = 4,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Dinosaur").youControl())
        )
        description = "Dinosaurs you control get +4/+4 until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "285"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a5a6f7b-149d-46d3-9814-d38a302db17c.jpg?1783935684"
        ruling(
            "2017-09-29",
            "If either creature is an illegal target as Huatli's second ability resolves, the creature " +
                "you control won't deal damage.",
        )
        ruling(
            "2017-09-29",
            "Huatli's last ability affects only Dinosaurs you control at the time it resolves. Dinosaurs " +
                "you begin to control later in the turn or creatures that become Dinosaurs later in the " +
                "turn won't get +4/+4.",
        )
    }
}
