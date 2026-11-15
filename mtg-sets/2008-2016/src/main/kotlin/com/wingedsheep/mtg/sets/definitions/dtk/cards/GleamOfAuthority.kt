package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Gleam of Authority
 * {1}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 for each +1/+1 counter on other creatures you control.
 * Enchanted creature has vigilance and "{W}, {T}: Bolster 1." (To bolster 1, choose a creature
 * with the least toughness among creatures you control and put a +1/+1 counter on it.)
 *
 * "Other" is relative to the enchanted creature: inside an Aura's granted bonus `excludeSelf`
 * leaves out the affected entity (the enchanted creature), not the Aura. "You" is the Aura's
 * controller.
 */
val GleamOfAuthority = card("Gleam of Authority") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 for each +1/+1 counter on other creatures you control.\n" +
        "Enchanted creature has vigilance and \"{W}, {T}: Bolster 1.\" (To bolster 1, choose a " +
        "creature with the least toughness among creatures you control and put a +1/+1 counter on it.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature, excludeSelf = true)
                .totalCounters(CounterType.PLUS_ONE_PLUS_ONE),
            toughnessBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature, excludeSelf = true)
                .totalCounters(CounterType.PLUS_ONE_PLUS_ONE)
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap),
                effect = Patterns.Mechanic.bolster(1)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Jakub Kasper"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96847438-eb09-4e3f-8ebf-f064b88f70c9.jpg?1783938617"
    }
}
