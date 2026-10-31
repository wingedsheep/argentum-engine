package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ajani, Inspiring Leader
 * {4}{W}{W}
 * Legendary Planeswalker — Ajani
 * Starting Loyalty: 5
 *
 * +2: You gain 2 life. Put two +1/+1 counters on up to one target creature.
 * −3: Exile target creature. Its controller gains 2 life.
 * −10: Creatures you control gain flying and double strike until end of turn.
 *
 * The +2 may be activated with no target (you just gain 2 life); a chosen target that becomes
 * illegal fizzles the whole ability, so no life is gained either. The −3's life gain goes to the
 * creature's controller, read before it is exiled (Swords to Plowshares' shape). The −10 locks in
 * the creatures you control as it resolves and grants both keywords in one pass over that group.
 */
val AjaniInspiringLeader = card("Ajani, Inspiring Leader") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Ajani"
    startingLoyalty = 5
    oracleText = "+2: You gain 2 life. Put two +1/+1 counters on up to one target creature.\n" +
        "−3: Exile target creature. Its controller gains 2 life.\n" +
        "−10: Creatures you control gain flying and double strike until end of turn."

    loyaltyAbility(+2) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.GainLife(2) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
        description = "You gain 2 life. Put two +1/+1 counters on up to one target creature."
    }

    loyaltyAbility(-3) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainLife(2, EffectTarget.TargetController) then
            Effects.Exile(creature)
        description = "Exile target creature. Its controller gains 2 life."
    }

    loyaltyAbility(-10) {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.IterationEntity)
        )
        description = "Creatures you control gain flying and double strike until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "282"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dba41e5f-66b8-4459-8a07-bbe893216f1e.jpg?1783932923"
        ruling(
            "2019-07-12",
            "You may activate Ajani's first ability without choosing any target. If you do, you will gain 2 life. " +
                "However, if you do choose a target and the target creature is an illegal target by the time the " +
                "ability tries to resolve, the ability doesn't resolve. You won't gain 2 life."
        )
        ruling(
            "2019-07-12",
            "If the target creature is an illegal target by the time Ajani's second ability tries to resolve, " +
                "the ability doesn't resolve. No player will gain life."
        )
        ruling(
            "2019-07-12",
            "Ajani's last ability affects only creatures you control at the time it resolves. Creatures you " +
                "begin to control later in the turn won't gain flying or double strike."
        )
    }
}
