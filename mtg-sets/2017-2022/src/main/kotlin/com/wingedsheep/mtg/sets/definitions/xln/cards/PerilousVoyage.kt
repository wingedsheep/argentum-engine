package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Perilous Voyage — Ixalan #67 (uncommon)
 * {1}{U} · Instant
 *
 * Return target nonland permanent you don't control to its owner's hand. If its mana value was
 * 2 or less, scry 2.
 *
 * The mana-value check runs after the bounce, so it reads the permanent's last-known information
 * (its `LastKnownPermanentComponent` snapshot) — the mana value it had on the battlefield, per the
 * 2017-09-29 ruling. If the target is illegal on resolution the whole spell fizzles and there is
 * no scry.
 */
val PerilousVoyage = card("Perilous Voyage") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target nonland permanent you don't control to its owner's hand. " +
        "If its mana value was 2 or less, scry 2."

    spell {
        val permanent = target(
            TargetFilter(
                GameObjectFilter.NonlandPermanent.withControllerPredicate(
                    ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.ReturnToHand(permanent) then
            Effects.If(
                condition = Conditions.TargetSpellManaValueAtMost(DynamicAmounts.fixed(2), permanent),
                then = Patterns.Library.scry(2)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "Wesley Burt"
        flavorText = "For the first time in her life, Vraska tried to prevent death."
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9f6623e-2784-4bc9-9d8c-3eee9c78c350.jpg?1783935777"
        ruling(
            "2017-09-29",
            "If the target permanent is an illegal target by the time Perilous Voyage resolves, " +
                "the entire spell doesn't resolve. You won't scry."
        )
        ruling(
            "2017-09-29",
            "Use the permanent's mana value as it existed on the battlefield to determine whether you scry."
        )
        ruling("2017-09-29", "If a permanent has {X} in its mana cost, X is considered to be 0.")
    }
}
