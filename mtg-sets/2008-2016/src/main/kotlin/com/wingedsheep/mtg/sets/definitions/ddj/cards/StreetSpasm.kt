package com.wingedsheep.mtg.sets.definitions.ddj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
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
 * Street Spasm — Duel Decks: Izzet vs. Golgari #30 (canonical printing; predates RTR by a month)
 * {X}{R} · Instant
 *
 * Street Spasm deals X damage to target creature without flying you don't control.
 * Overload {X}{X}{R}{R}
 *
 * Electrickery's shape with an X amount. Both casts read the chosen X via
 * [DynamicAmounts.xValue]: the overload cost carries {X}{X}, which the overload enumerator prices
 * at two generic per X (`ManaCost.withXAs` multiplies by `xCount`) while X itself stays the
 * announced value, so the overloaded spell deals X — not 2X — to each creature.
 */
val StreetSpasm = card("Street Spasm") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Street Spasm deals X damage to target creature without flying you don't control.\n" +
        "Overload {X}{X}{R}{R} (You may cast this spell for its overload cost. If you do, change \"target\" " +
        "in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{X}{X}{R}{R}"))

    spell {
        val groundedNotYours = GameObjectFilter.Creature
            .withoutKeyword(Keyword.FLYING)
            .withControllerPredicate(ControllerPredicate.Not(ControllerPredicate.ControlledByYou))
        val creature = target(TargetFilter(groundedNotYours))
        effect = Effects.DealDamage(DynamicAmounts.xValue(), creature)

        overloadEffect = Patterns.Group.dealDamageToAll(DynamicAmounts.xValue(), GroupFilter(groundedNotYours))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f599948f-1561-415f-b415-c9c991896704.jpg?1783940411"
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
        ruling(
            "2024-01-12",
            "If you are instructed to cast a spell with overload \"without paying its mana cost,\" you can't " +
                "choose to pay its overload cost instead."
        )
    }
}
