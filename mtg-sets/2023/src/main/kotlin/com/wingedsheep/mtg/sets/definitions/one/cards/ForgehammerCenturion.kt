package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Forgehammer Centurion
 * {2}{R}
 * Creature — Phyrexian Warrior
 * 3/2
 *
 * Whenever another creature or artifact you control is put into a graveyard from the
 * battlefield, put an oil counter on this creature.
 * Whenever this creature attacks, you may remove two oil counters from it. When you do,
 * target creature can't block this turn.
 *
 * "When you do" is a reflexive trigger (CR 603.12): the target is chosen as it goes on the
 * stack, after the counters are removed. Removing two is all-or-nothing — the reflexive
 * executor's feasibility check only offers the may-clause when at least two oil counters are
 * on the creature, so a lone counter can't buy the payoff.
 */
val ForgehammerCenturion = card("Forgehammer Centurion") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phyrexian Warrior"
    power = 3
    toughness = 2
    oracleText = "Whenever another creature or artifact you control is put into a graveyard from the " +
        "battlefield, put an oil counter on this creature.\n" +
        "Whenever this creature attacks, you may remove two oil counters from it. When you do, " +
        "target creature can't block this turn."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.CreatureOrArtifact.youControl()).dies()
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ReflexiveTrigger(
            action = Effects.RemoveCounters(CounterType.OIL, 2, EffectTarget.Self),
            optional = true,
            descriptionOverride = "You may remove two oil counters from Forgehammer Centurion. " +
                "When you do, target creature can't block this turn."
        ) {
            val creature = target(TargetFilter.Creature)
            effect = Effects.CantBlock(creature)
        }
        description = "Whenever this creature attacks, you may remove two oil counters from it. " +
            "When you do, target creature can't block this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Vladimir Krisetskiy"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4c2c914-e9a7-450b-88d9-8885fe0f6643.jpg?1783918031"
    }
}
