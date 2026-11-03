package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Steadfast Armasaur
 * {3}{W}
 * Creature — Dinosaur
 * 2/3
 * Vigilance
 * {1}{W}, {T}: This creature deals damage equal to its toughness to target creature blocking or
 * blocked by it.
 *
 * The target is combat-paired with the source via [GameObjectFilter.blockingOrBlockedBySource];
 * the damage is the source's toughness read at resolution.
 */
val SteadfastArmasaur = card("Steadfast Armasaur") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Dinosaur"
    power = 2
    toughness = 3
    oracleText = "Vigilance\n{1}{W}, {T}: This creature deals damage equal to its toughness to " +
        "target creature blocking or blocked by it."

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap)
        val paired = target(TargetFilter(GameObjectFilter.Creature.blockingOrBlockedBySource()))
        effect = Effects.DealDamage(DynamicAmounts.sourceToughness(), paired)
        description = "Steadfast Armasaur deals damage equal to its toughness to target creature " +
            "blocking or blocked by it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "39"
        artist = "Greg Opalinski"
        flavorText = "\"Like the mighty armasaur, we will defend against all who invade our shores.\"\n—Itzama the Crested"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/197934d5-726c-4cd3-a934-3d6449a5a56e.jpg?1783935790"
    }
}
