package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Heliod's Punishment — Theros Beyond Death #21.
 *
 * {1}{W} Enchantment — Aura
 * Enchant creature
 * This Aura enters with four task counters on it.
 * Enchanted creature can't attack or block. It loses all abilities and has "{T}: Remove a task
 * counter from Heliod's Punishment. Then if it has no task counters on it, destroy Heliod's
 * Punishment."
 *
 * The granted ability names the Aura, not its bearer, so it reaches the Aura through
 * [EffectTarget.GrantingSource]. A creature under two Punishments has the ability once, granted by
 * the most recently attached one, so only that Aura counts down (ruling) — duplicate grants keep
 * the latest-timestamp granter.
 */
val HeliodsPunishment = card("Heliod's Punishment") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "This Aura enters with four task counters on it.\n" +
        "Enchanted creature can't attack or block. It loses all abilities and has \"{T}: Remove a " +
        "task counter from Heliod's Punishment. Then if it has no task counters on it, destroy " +
        "Heliod's Punishment.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.TASK,
            count = 4,
            selfOnly = true
        )
    )

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = LoseAllAbilities()
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                effect = Effects.RemoveCounters(CounterType.TASK, 1, EffectTarget.GrantingSource) then
                    Effects.If(
                        condition = Conditions.Not(
                            Conditions.TargetHasCounter(CounterType.TASK, EffectTarget.GrantingSource)
                        ),
                        then = Effects.Destroy(EffectTarget.GrantingSource),
                    )
                description = "{T}: Remove a task counter from Heliod's Punishment. Then if it has no " +
                    "task counters on it, destroy Heliod's Punishment."
            },
            filter = Filters.EnchantedCreature
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b8e6878-06aa-4505-b3a5-172083b7d258.jpg?1783931596"
        ruling("2020-01-24", "If the affected creature gains an ability after Heliod's Punishment becomes attached to it, it will keep that ability.")
        ruling("2020-01-24", "If the enchanted creature has an ability that defines a * in its power or toughness, that * is 0.")
        ruling("2020-01-24", "If a creature is enchanted by two or more Heliod's Punishments, it has only the activated ability of the one that most recently became attached. Activating that ability removes a task counter from only that one.")
    }
}
