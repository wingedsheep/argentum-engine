package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Behind the Scenes
 * {2}{B}
 * Enchantment
 * Creatures you control have skulk. (They can't be blocked by creatures with greater power.)
 * {4}{W}: Creatures you control get +1/+1 until end of turn.
 */
val BehindTheScenes = card("Behind the Scenes") {
    manaCost = "{2}{B}"
    colorIdentity = "WB"
    typeLine = "Enchantment"
    oracleText = "Creatures you control have skulk. (They can't be blocked by creatures with greater power.)\n" +
        "{4}{W}: Creatures you control get +1/+1 until end of turn."

    staticAbility {
        ability = GrantKeyword(Keyword.SKULK, GroupFilter.AllCreaturesYouControl)
    }

    activatedAbility {
        cost = Costs.Mana("{4}{W}")
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "David Gaillet"
        flavorText = "It does no good to bar the gate when the enemy is already inside."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fac171d-a8b6-4bb7-a559-473574aa02a8.jpg?1783937780"

        ruling("2016-04-08", "Skulk matters only as blockers are chosen. Modifying either creature's power after blockers are chosen won't cause the attacking creature to become unblocked.")
        ruling("2016-04-08", "If you cause a creature to have 0 power or less, use the actual value (which may be negative) to determine whether it can block or be blocked. A creature with skulk and 0 or less power most likely won't be blocked, but it won't deal combat damage and won't trigger any abilities that trigger when combat damage is dealt.")
    }
}
