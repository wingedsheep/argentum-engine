package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hankyu
 * {1}
 * Artifact — Equipment
 *
 * Equipped creature has "{T}: Put an aim counter on Hankyu" and "{T}, Remove all aim counters
 * from Hankyu: This creature deals damage to any target equal to the number of aim counters
 * removed this way."
 * Equip {4}
 *
 * The aim counters live on the **Equipment**, so both granted abilities name the granter
 * ([EffectTarget.GrantingSource], [Costs.RemoveAllCountersFromGrantingPermanent]) while the
 * damage comes from the **equipped creature** (the ability's source). The counters are gone by
 * resolution, so the damage reads how many the cost removed, not what Hankyu has left.
 */
val Hankyu = card("Hankyu") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has \"{T}: Put an aim counter on Hankyu\" and \"{T}, Remove all " +
        "aim counters from Hankyu: This creature deals damage to any target equal to the number of " +
        "aim counters removed this way.\"\n" +
        "Equip {4} ({4}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                effect = Effects.AddCounters(CounterType.AIM, 1, EffectTarget.GrantingSource)
            }
        )
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(
                    Costs.Tap,
                    Costs.RemoveAllCountersFromGrantingPermanent(CounterType.AIM),
                )
                val t = target(Targets.Any)
                effect = Effects.DealDamage(
                    amount = DynamicAmounts.countersRemovedAsCost(),
                    target = t,
                    damageSource = EffectTarget.Self,
                )
            }
        )
    }

    equipAbility("{4}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "253"
        artist = "Ben Thompson"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f22a88a-4f04-4500-9e05-909b54ad43e3.jpg?1783944279"
    }
}
