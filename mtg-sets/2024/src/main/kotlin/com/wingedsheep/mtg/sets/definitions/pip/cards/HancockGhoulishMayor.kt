package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hancock, Ghoulish Mayor
 *
 * The lord bonus reads the live count of counters of *every* kind on Hancock
 * ([DynamicAmounts.countersOnSelf] with a `null` type), so the +1/+1 counter Undying returns it
 * with, proliferated counters, or any other kind all grow the team. `excludeSelf` keeps
 * Hancock from pumping itself (cf. [com.wingedsheep.mtg.sets.definitions.inv.cards.KangeeAerieKeeper]).
 */
val HancockGhoulishMayor = card("Hancock, Ghoulish Mayor") {
    manaCost = "{2}{B}"
    typeLine = "Legendary Creature — Zombie Mutant Advisor"
    power = 2
    toughness = 1
    oracleText = "Each other creature you control that's a Zombie or Mutant gets +X/+X, where X is the number of counters on Hancock.\n" +
        "Undying (When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield under its owner's control with a +1/+1 counter on it.)"

    keywords(Keyword.UNDYING)

    staticAbility {
        val counterCount = DynamicAmounts.countersOnSelf(null)
        ability = GrantDynamicStats(
            filter = GroupFilter(
                GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.ZOMBIE, Subtype.MUTANT)).youControl(),
                excludeSelf = true
            ),
            powerBonus = counterCount,
            toughnessBonus = counterCount
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "Wonchun Choi"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39bda1b4-3f02-4cbc-9573-f5b6e6c991d5.jpg?1783912408"
    }
}
