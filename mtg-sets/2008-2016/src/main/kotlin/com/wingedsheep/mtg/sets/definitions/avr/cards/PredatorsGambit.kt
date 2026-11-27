package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.DynamicAmounts

/**
 * Predator's Gambit
 * {B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+1.
 * Enchanted creature has intimidate as long as its controller controls no other creatures.
 *
 * "Its controller" is the enchanted creature's controller, not the Aura's — so the check is
 * `StatePredicate.ControllerMeets` on the enchanted creature, which rebinds the condition's "you"
 * to that creature's controller.
 *
 * "No other creatures" is read as "at most one creature": the filter already requires the
 * enchanted permanent to be a creature that player controls, so the only creature the count may
 * see is the enchanted one itself. Counting this way keeps the check free of the source-relative
 * "other than enchanted creature" predicate, which needs the Aura as context the nested condition
 * doesn't carry.
 */
val PredatorsGambit = card("Predator's Gambit") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+1.\n" +
        "Enchanted creature has intimidate as long as its controller controls no other creatures. " +
        "(It can't be blocked except by artifact creatures and/or creatures that share a color with it.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility { ability = ModifyStats(2, 1) }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INTIMIDATE),
            condition = Conditions.EnchantedPermanentMatches(
                GameObjectFilter.Creature.controllerMeets(
                    Conditions.CompareAmounts(
                        DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count(),
                        ComparisonOperator.LTE,
                        1
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88810a96-d5f8-4030-93f1-e2ad0d480317.jpg?1783940692"
    }
}
