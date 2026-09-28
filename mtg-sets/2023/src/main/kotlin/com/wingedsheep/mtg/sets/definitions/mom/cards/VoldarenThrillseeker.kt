package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Voldaren Thrillseeker
 * {2}{R}
 * Creature — Vampire Warrior
 * 1/1
 * Backup 2
 * {1}, Sacrifice this creature: It deals damage equal to its power to any target.
 */
val VoldarenThrillseeker = card("Voldaren Thrillseeker") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire Warrior"
    oracleText = "Backup 2 (When this creature enters, put two +1/+1 counters on target creature. If that's " +
        "another creature, it gains the following ability until end of turn.)\n" +
        "{1}, Sacrifice this creature: It deals damage equal to its power to any target."
    power = 1
    toughness = 1

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantActivatedAbility(
                    ability = grantedActivatedAbility {
                        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
                        val anyTarget = target(Targets.Any)
                        effect = Effects.DealDamage(
                            amount = DynamicAmounts.sourcePower(),
                            target = anyTarget,
                            damageSource = EffectTarget.Self,
                        )
                    },
                    target = creature,
                ),
            )
        description = "Backup 2 (When this creature enters, put two +1/+1 counters on target creature. If that's " +
            "another creature, it gains the following ability until end of turn.)"
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.sourcePower(),
            target = anyTarget,
            damageSource = EffectTarget.Self,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Viko Menezes"
        flavorText = "\"The end is always nigh. Let's have some fun!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1dbcd466-fb99-4388-a118-4534b85544f0.jpg?1783916978"
    }
}
