package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hydra Trainer
 * {1}{G}
 * Creature — Human Warrior
 * 1/1
 *
 * You may exert this creature as it attacks. When you do, target creature gets +X/+X until end of
 * turn, where X is the number of counters on permanents you control.
 * {2}{G}: Adapt 2.
 *
 * The exert is an optional cost to attack ([ExertAsItAttacks], CR 701.43d); the "when you do"
 * paragraph is the trigger linked to it (CR 607.2h). X is counted on resolution.
 */
val HydraTrainer = card("Hydra Trainer") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    oracleText = "You may exert this creature as it attacks. When you do, target creature gets +X/+X until " +
        "end of turn, where X is the number of counters on permanents you control. (An exerted creature " +
        "won't untap during your next untap step.)\n" +
        "{2}{G}: Adapt 2. (If this creature has no +1/+1 counters on it, put two +1/+1 counters on it.)"
    power = 1
    toughness = 1

    staticAbility {
        ability = ExertAsItAttacks
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        val creature = target(TargetFilter.Creature)
        val x = DynamicAmounts.battlefield(Player.You).totalCounters()
        effect = Effects.ModifyStats(x, x, creature)
    }

    // {2}{G}: Adapt 2.
    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        effect = Effects.If(
            Conditions.Not(Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE)),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d28ae5c-eec3-445a-81d6-42bf9789afce.jpg?1783911259"
    }
}
