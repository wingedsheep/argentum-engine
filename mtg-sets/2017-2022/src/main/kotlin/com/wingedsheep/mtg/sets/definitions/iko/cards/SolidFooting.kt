package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Solid Footing
 * {W}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * Enchanted creature gets +1/+1.
 * As long as enchanted creature has vigilance, it assigns combat damage equal to its toughness
 * rather than its power.
 *
 * The vigilance gate rides on the filter rather than a `ConditionalStaticAbility`: an
 * [AssignDamageEqualToToughness] scanned off the battlefield (`CombatDamageUtils.matchesBattlefield`)
 * checks its full filter against projected state, so "the creature this Aura is attached to
 * (`attachedToBySource`) that has vigilance" re-evaluates at every damage assignment and follows
 * vigilance granted or lost mid-turn.
 */
val SolidFooting = card("Solid Footing") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "Enchanted creature gets +1/+1.\n" +
        "As long as enchanted creature has vigilance, it assigns combat damage equal to its toughness " +
        "rather than its power."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = AssignDamageEqualToToughness(
            filter = GroupFilter(
                GameObjectFilter.Creature.withKeyword(Keyword.VIGILANCE).attachedToBySource(),
            ),
            onlyWhenToughnessGreaterThanPower = false,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/0900a8d1-a1b1-48c9-8af1-726a623c7d74.jpg?1783931082"
    }
}
