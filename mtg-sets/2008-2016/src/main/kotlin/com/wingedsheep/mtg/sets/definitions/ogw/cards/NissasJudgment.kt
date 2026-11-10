package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nissa's Judgment — Oath of the Gatewatch #139 (canonical printing)
 * {4}{G} · Sorcery
 *
 * Support 2. (Put a +1/+1 counter on each of up to two target creatures.)
 * Choose up to one target creature an opponent controls. Each creature you control with a +1/+1
 * counter on it deals damage equal to its power to that creature.
 *
 * Two target declarations: support's "up to two target creatures" (any controller) and "up to one
 * target creature an opponent controls". Because the spell has targets besides the supported ones,
 * the counters walk only the support declaration via [Effects.ForEachTargetOf] — a bare
 * `ForEachTarget` would also put a counter on the opposing target.
 *
 * Support finishes before any damage (ruling), and the damage group is gathered afterwards, so a
 * creature you control that just got its counter joins in with its boosted power. Each creature
 * deals its own damage (`damageSource = IterationEntity`, Moonlight Hunt's shape), so deathtouch,
 * lifelink and "whenever this deals damage" triggers key off the individual creature.
 */
val NissasJudgment = card("Nissa's Judgment") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Support 2. (Put a +1/+1 counter on each of up to two target creatures.)\n" +
        "Choose up to one target creature an opponent controls. Each creature you control with a " +
        "+1/+1 counter on it deals damage equal to its power to that creature."

    spell {
        val supported = targets(TargetFilter.Creature, count = 2, optional = true)
        val victim = target(TargetFilter.CreatureOpponentControls, optional = true)

        effect = Effects.ForEachTargetOf(
            supported,
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        ) then Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE).youControl()),
            effect = Effects.DealDamage(
                amount = DynamicAmounts.powerOf(EffectTarget.IterationEntity),
                target = victim,
                damageSource = EffectTarget.IterationEntity,
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96b263fb-00b5-4050-85e1-cfc4018bb75d.jpg?1783937901"
        ruling("2016-01-22", "You finish the support action before any creatures deal damage. Creatures that get a +1/+1 counter will deal damage to the creature an opponent controls, if applicable.")
        ruling("2016-01-22", "As Nissa's Judgment resolves, if at least one of its targets is still legal, it will resolve, affecting only targets that are still legal at that time. If none of its targets are still legal at that time, it won't resolve and none of its effects will happen.")
        ruling("2016-01-22", "You can't put more than one +1/+1 counter on any one target using the support action.")
        ruling("2016-01-22", "Support can target a creature you don't control.")
        ruling("2016-01-22", "If a spell with support has other abilities that target creatures, those abilities and the support ability can target the same creature.")
    }
}
