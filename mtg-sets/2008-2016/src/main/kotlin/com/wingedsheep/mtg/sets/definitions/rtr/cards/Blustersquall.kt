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
 * Blustersquall — Return to Ravnica #30 (canonical printing)
 * {U} · Instant
 *
 * Tap target creature you don't control.
 * Overload {3}{U}
 *
 * Electrickery's shape: "you don't control" is `Not(ControlledByYou)`, and the overloaded
 * version taps each such creature without targeting.
 */
val Blustersquall = card("Blustersquall") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap target creature you don't control.\n" +
        "Overload {3}{U} (You may cast this spell for its overload cost. If you do, change \"target\" " +
        "in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{3}{U}"))

    spell {
        val notYours = GameObjectFilter.Creature.withControllerPredicate(
            ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
        )
        val creature = target(TargetFilter(notYours))
        effect = Effects.Tap(creature)

        overloadEffect = Patterns.Group.tapAll(GroupFilter(notYours))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Willian Murai"
        flavorText = "Weather is more predictable than the Izzet."
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d998847b-323d-406a-b08e-0da66edcc7b3.jpg?1783940371"
        ruling(
            "2024-01-12",
            "Because a spell with overload doesn't target when its overload cost is paid, it may affect " +
                "permanents with hexproof or with protection from the appropriate color."
        )
    }
}
