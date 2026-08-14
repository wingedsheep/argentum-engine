package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Galvanize — Murders at Karlov Manor #128
 * {1}{R} · Instant
 *
 * Galvanize deals 3 damage to target creature. If you've drawn two or more cards this turn,
 * Galvanize deals 5 damage to that creature instead.
 *
 * One damage event whose amount is a [DynamicAmount.Conditional] over
 * [Conditions.YouDrewCardsThisTurn] — not two competing effects. The condition is evaluated as
 * Galvanize resolves (CR 608.2), so a draw made after casting but before resolution counts, and
 * the "instead" replaces the amount rather than adding a second damage instance.
 */
val Galvanize = card("Galvanize") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Galvanize deals 3 damage to target creature. If you've drawn two or more cards " +
        "this turn, Galvanize deals 5 damage to that creature instead."

    spell {
        val t = target("target creature", Targets.Creature)
        effect = Effects.DealDamage(
            DynamicAmount.Conditional(
                condition = Conditions.YouDrewCardsThisTurn(2),
                ifTrue = DynamicAmount.Fixed(5),
                ifFalse = DynamicAmount.Fixed(3)
            ),
            t
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "Matt Forsyth"
        flavorText = "\"Why doesn't Kylox put an off switch on these things?!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64ed3bfa-3294-45dd-825e-3afc2580f0d4.jpg?1783912882"
    }
}
