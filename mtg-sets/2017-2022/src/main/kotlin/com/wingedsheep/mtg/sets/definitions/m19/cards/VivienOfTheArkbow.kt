package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vivien of the Arkbow — Core Set 2019 #301 (canonical printing)
 * {4}{G}{G} · Legendary Planeswalker — Vivien · Loyalty 5
 *
 * - +2: Tyvar Kell's optional-target counter shape ("up to one target creature").
 * - −3: Hunter's Edge's bite — the creature you control is the damage source.
 * - −9: Overrun's per-creature pump, locked to the creatures you control on resolution (ruling).
 */
val VivienOfTheArkbow = card("Vivien of the Arkbow") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Vivien"
    startingLoyalty = 5
    oracleText = "+2: Put two +1/+1 counters on up to one target creature.\n" +
        "−3: Target creature you control deals damage equal to its power to target creature you don't control.\n" +
        "−9: Creatures you control get +4/+4 and gain trample until end of turn."

    loyaltyAbility(+2) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 2, target = creature)
    }

    loyaltyAbility(-3) {
        val yours = target(TargetFilter.Creature.youControl())
        val theirs = target(TargetFilter.Creature.opponentControls())
        effect = Effects.DealDamage(DynamicAmounts.powerOf(yours), theirs, damageSource = yours)
    }

    loyaltyAbility(-9) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(4, 4, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "301"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c185555-acd4-461a-821a-b21b7b0914a5.jpg?1783934487"
        ruling("2018-07-13", "If either creature is an illegal target as Vivien's second ability tries to resolve, the creature you control won't deal damage.")
        ruling("2018-07-13", "Vivien's last ability affects only creatures you control at the time it resolves. Creatures you begin to control later in the turn won't get +4/+4 or gain trample.")
    }
}
