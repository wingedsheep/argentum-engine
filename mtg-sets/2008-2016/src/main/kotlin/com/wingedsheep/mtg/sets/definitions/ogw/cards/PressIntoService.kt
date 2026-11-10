package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Press into Service — Oath of the Gatewatch #114 (canonical printing)
 * {4}{R} · Sorcery
 *
 * Support 2. (Put a +1/+1 counter on each of up to two target creatures.)
 * Gain control of target creature until end of turn. Untap that creature. It gains haste until
 * end of turn.
 *
 * Two target declarations: support's "up to two target creatures" and the required threaten
 * target. The counters walk only the support declaration via [Effects.ForEachTargetOf] (Nissa's
 * Judgment's shape), so the stolen creature gets a counter only when it was also chosen as a
 * support target — which the rulings allow — and never just for being the threaten target.
 */
val PressIntoService = card("Press into Service") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Support 2. (Put a +1/+1 counter on each of up to two target creatures.)\n" +
        "Gain control of target creature until end of turn. Untap that creature. It gains haste " +
        "until end of turn."

    spell {
        val supported = targets(TargetFilter.Creature, count = 2, optional = true)
        val conscript = target(TargetFilter.Creature)

        effect = Effects.ForEachTargetOf(
            supported,
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        ) then Effects.GainControl(conscript, Duration.EndOfTurn) then
            Effects.Untap(conscript) then
            Effects.GrantKeyword(Keyword.HASTE, conscript)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69b9eb4b-3ab8-4506-baa4-3a818e7677a3.jpg?1783937905"
        ruling("2016-01-22", "The last target of Press into Service can be any creature, even one that's untapped, one you already control, or one targeted by the support part of the spell.")
        ruling("2016-01-22", "Gaining control of a creature doesn't cause you to gain control of any Auras or Equipment attached to it.")
        ruling("2016-01-22", "As Press into Service resolves, if at least one of its targets is still legal, it will resolve, affecting only targets that are still legal at that time. If none of its targets are still legal at that time, it won't resolve and none of its effects will happen.")
        ruling("2016-01-22", "You can't put more than one +1/+1 counter on any one target using the support action.")
        ruling("2016-01-22", "Support can target a creature you don't control.")
        ruling("2016-01-22", "If a spell with support has other abilities that target creatures, those abilities and the support ability can target the same creature.")
    }
}
