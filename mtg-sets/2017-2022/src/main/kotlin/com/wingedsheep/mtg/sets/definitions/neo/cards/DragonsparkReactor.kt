package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragonspark Reactor — Kamigawa: Neon Dynasty #137 (canonical printing)
 * {1}{R} · Artifact
 *
 * Whenever this artifact or another artifact you control enters, put a charge counter on this
 * artifact.
 * {4}, Sacrifice this artifact: It deals damage equal to the number of charge counters on it to
 * target player and that much damage to up to one target creature.
 *
 * The sacrifice is a cost, so the counters are gone by resolution: the amount is the pre-cost
 * charge count, read as last-known information via `DynamicAmount.LastKnownSourceCounters`.
 */
val DragonsparkReactor = card("Dragonspark Reactor") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "Whenever this artifact or another artifact you control enters, put a charge " +
        "counter on this artifact.\n" +
        "{4}, Sacrifice this artifact: It deals damage equal to the number of charge counters on " +
        "it to target player and that much damage to up to one target creature."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.youControl()).enters()
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.SacrificeSelf)
        val player = target(Targets.Player)
        val creature = target(TargetFilter.Creature, optional = true)
        val charge = DynamicAmounts.lastKnownSourceCounters(CounterType.CHARGE)
        effect = Effects.DealDamage(charge, player) then Effects.DealDamage(charge, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Billy Christian"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cf531dd-0a93-43e3-880b-f705fa5f533d.jpg?1783923869"
    }
}
