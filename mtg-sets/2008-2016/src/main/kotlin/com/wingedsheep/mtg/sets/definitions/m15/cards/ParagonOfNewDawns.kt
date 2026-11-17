package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Paragon of New Dawns
 * {3}{W}
 * Creature — Human Soldier
 * 2/2
 * Other white creatures you control get +1/+1.
 * {W}, {T}: Another target white creature you control gains vigilance until end of turn. (Attacking doesn't cause it to tap.)
 */
val ParagonOfNewDawns = card("Paragon of New Dawns") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Other white creatures you control get +1/+1.\n" +
        "{W}, {T}: Another target white creature you control gains vigilance until end of turn. (Attacking doesn't cause it to tap.)"

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withColor(Color.WHITE).youControl(),
                excludeSelf = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        val creature = target(TargetFilter.Creature.withColor(Color.WHITE).youControl().other())
        effect = Effects.GrantKeyword(Keyword.VIGILANCE, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "John Stanko"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/361c66c3-b4eb-48aa-a967-255a8db60977.jpg"
    }
}
