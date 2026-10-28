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
 * Electrickery — Return to Ravnica #93 (canonical printing)
 * {R} · Instant
 *
 * Electrickery deals 1 damage to target creature you don't control.
 * Overload {1}{R}
 *
 * Cyclonic Rift's shape: "you don't control" is `Not(ControlledByYou)`, and the overloaded
 * version damages each such creature without targeting.
 */
val Electrickery = card("Electrickery") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Electrickery deals 1 damage to target creature you don't control.\n" +
        "Overload {1}{R} (You may cast this spell for its overload cost. If you do, change \"target\" " +
        "in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{1}{R}"))

    spell {
        val notYours = GameObjectFilter.Creature.withControllerPredicate(
            ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
        )
        val creature = target(TargetFilter(notYours))
        effect = Effects.DealDamage(1, creature)

        overloadEffect = Patterns.Group.dealDamageToAll(1, GroupFilter(notYours))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5ed81ee8-d5e4-4127-876e-9bff81f9c726.jpg?1783940356"
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
