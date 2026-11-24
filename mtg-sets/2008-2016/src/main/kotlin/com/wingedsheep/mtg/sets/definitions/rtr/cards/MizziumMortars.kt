package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Mizzium Mortars — Return to Ravnica #101 (canonical printing)
 * {1}{R} · Sorcery
 *
 * Mizzium Mortars deals 4 damage to target creature you don't control.
 * Overload {3}{R}{R}{R}
 *
 * Electrickery's shape: "you don't control" is `Not(ControlledByYou)`, and the overloaded
 * version damages each such creature without targeting (CR 702.96b).
 */
val MizziumMortars = card("Mizzium Mortars") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Mizzium Mortars deals 4 damage to target creature you don't control.\n" +
        "Overload {3}{R}{R}{R} (You may cast this spell for its overload cost. If you do, change \"target\" " +
        "in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{3}{R}{R}{R}"))

    spell {
        val notYours = GameObjectFilter.Creature.withControllerPredicate(
            ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
        )
        val creature = target(TargetFilter(notYours))
        effect = Effects.DealDamage(4, creature)

        overloadEffect = Patterns.Group.dealDamageToAll(4, GroupFilter(notYours))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "101"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4ded88d-2688-4f5e-a8b2-16216cf9c792.jpg?1783940354"
        ruling(
            "2024-01-12",
            "Because a spell with overload doesn't target when its overload cost is paid, it may affect " +
                "permanents with hexproof or with protection from the appropriate color."
        )
        ruling(
            "2024-01-12",
            "Note that if the spell with overload is dealing damage, protection from that spell's color " +
                "will still prevent that damage."
        )
    }
}
