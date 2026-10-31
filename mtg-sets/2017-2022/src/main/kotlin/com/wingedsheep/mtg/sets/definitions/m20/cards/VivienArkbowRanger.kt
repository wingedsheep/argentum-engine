package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Vivien, Arkbow Ranger
 * {1}{G}{G}{G}
 * Legendary Planeswalker — Vivien
 * Loyalty: 4
 *
 * +1: Distribute two +1/+1 counters among up to two target creatures. They gain trample until end of turn.
 * −3: Target creature you control deals damage equal to its power to target creature or planeswalker.
 * −5: You may reveal a creature card you own from outside the game and put it into your hand.
 *
 * The +1 is Ajani, Sleeper Agent's −3 shape: zero to two targets, an exact division announced with
 * the activation (each chosen target gets at least one counter, so two targets get one each), then
 * every target gains trample. The −3 is Master's Rebuke's bite; the −5 is the creature wish
 * ([Patterns.Sideboard.wish]), whose "may" is the up-to-one choice.
 */
val VivienArkbowRanger = card("Vivien, Arkbow Ranger") {
    manaCost = "{1}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Vivien"
    startingLoyalty = 4
    oracleText = "+1: Distribute two +1/+1 counters among up to two target creatures. They gain trample until end of turn.\n" +
        "−3: Target creature you control deals damage equal to its power to target creature or planeswalker.\n" +
        "−5: You may reveal a creature card you own from outside the game and put it into your hand."

    loyaltyAbility(+1) {
        target = TargetObject(filter = TargetFilter.Creature, count = 2, minCount = 0)
        effect = Effects.DistributeCountersAmongTargets(totalCounters = 2) then
            Effects.ForEachTarget(Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.ContextTarget(0)))
        description = "Distribute two +1/+1 counters among up to two target creatures. They gain trample until end of turn."
    }

    loyaltyAbility(-3) {
        val source = target(TargetFilter.Creature.youControl())
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker))
        effect = Effects.DealDamage(DynamicAmounts.powerOf(source), victim, damageSource = source)
        description = "Target creature you control deals damage equal to its power to target creature or planeswalker."
    }

    loyaltyAbility(-5) {
        effect = Patterns.Sideboard.wish(GameObjectFilter.Creature)
        description = "You may reveal a creature card you own from outside the game and put it into your hand."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "199"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/592c5e27-c538-425e-b41f-1d6708428853.jpg?1783932954"
        ruling("2019-07-12", "You choose how the counters will be distributed as you activate Vivien's first ability. Each target creature must be assigned at least one counter. This means that you can't put two counters on one creature but give two creatures trample.")
        ruling("2019-07-12", "You can activate Vivien's first ability without choosing any target creatures. The counters won't be put on anything. This is a change from previous rules regarding distributing counters.")
        ruling("2019-07-12", "If one of the two target creatures becomes an illegal target in response to Vivien's first ability, the +1/+1 counter that would have been put on that creature is lost. It can't be put on the legal target.")
        ruling("2019-07-12", "If either target is an illegal target as Vivien's second ability tries to resolve, the creature you control won't deal damage.")
        ruling("2019-07-12", "In a casual game, a card you choose from outside the game comes from your personal collection. In a tournament event, a card you choose from outside the game must come from your sideboard. You may look at your sideboard at any time.")
    }
}
