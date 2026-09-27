package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodthirsty Ogre
 * {2}{B}
 * Creature — Ogre Warrior Shaman
 * 3/1
 * {T}: Put a devotion counter on this creature.
 * {T}: Target creature gets -X/-X until end of turn, where X is the number of devotion counters on
 * this creature. Activate only if you control a Demon.
 *
 * Devotion is a passive tally ([CounterType.DEVOTION]). X is read as the shrink resolves. "A Demon"
 * is the bare tribal noun — any permanent with the subtype.
 */
val BloodthirstyOgre = card("Bloodthirsty Ogre") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Ogre Warrior Shaman"
    power = 3
    toughness = 1
    oracleText = "{T}: Put a devotion counter on this creature.\n" +
        "{T}: Target creature gets -X/-X until end of turn, where X is the number of devotion counters " +
        "on this creature. Activate only if you control a Demon."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddCounters(CounterType.DEVOTION, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Tap
        val victim = target(TargetFilter.Creature)
        val shrink = -DynamicAmounts.countersOnSelf(CounterType.DEVOTION)
        effect = Effects.ModifyStats(shrink, shrink, victim)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.ControlPermanentOfType(Subtype("Demon")))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Thomas M. Baxa"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c557f035-f93b-41ce-b8de-dea79dcf15be.jpg?1783944317"
    }
}
