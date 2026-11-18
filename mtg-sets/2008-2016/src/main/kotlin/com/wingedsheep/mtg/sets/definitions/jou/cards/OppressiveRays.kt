package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackOrBlockUnlessPay
import com.wingedsheep.sdk.scripting.IncreaseActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Oppressive Rays
 * {W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature can't attack or block unless its controller pays {3}.
 * Activated abilities of enchanted creature cost {3} more to activate.
 *
 * The tax has no mana-ability carve-out, so it reaches a mana ability too: an enchanted Llanowar
 * Elves taps for {G} only by paying {3}, {T}. Auto-pay won't tap a taxed source; the ability is
 * offered at its total cost and activated by hand.
 */
val OppressiveRays = card("Oppressive Rays") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't attack or block unless its controller pays {3}.\n" +
        "Activated abilities of enchanted creature cost {3} more to activate."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttackOrBlockUnlessPay(amount = DynamicAmounts.fixed(3))
    }

    staticAbility {
        ability = IncreaseActivatedAbilityCost(
            filter = GroupFilter.attachedCreature(),
            amount = DynamicAmounts.fixed(3),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1eac995f-514d-4c65-8135-417f2b1a6fba.jpg?1783939457"
    }
}
