package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Farsight Ritual
 * {2}{U}{U}
 * Instant
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * Look at the top four cards of your library. If this spell was bargained, look at the top eight
 * cards of your library instead. Put two of them into your hand and the rest on the bottom of your
 * library in a random order.
 *
 * The spell-rider shape of bargain (CR 702.166c), like [ArchonsGlory] — except the payoff isn't an
 * extra clause but a *bigger number*, so it's a [DynamicAmount.Conditional] on
 * [Conditions.WasBargained] feeding the dig's count rather than a `ConditionalEffect` wrapping a
 * second effect. The bargained fact is stamped on the spell as it's cast and read while the spell
 * is still resolving.
 *
 * Only the look widens: `keepCount` stays a flat 2 per the WOE ruling ("You still put only two of
 * the cards into your hand if you bargain this spell").
 *
 * [Patterns.Library.lookAtTopAndKeep] is the whole dig, same recipe as Sinuous Benthisaur:
 * gather the top N off the library so the controller sees them privately (`revealed = false` — the
 * card says "look at", not "reveal"), `ChooseExactly(2)`, kept → hand, rest → library bottom with
 * [CardOrder.Random] for "in a random order". `ChooseExactly` caps at the collection size, so a
 * library of fewer than two cards simply keeps everything looked at rather than deadlocking.
 */
val FarsightRitual = card("Farsight Ritual") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "Look at the top four cards of your library. If this spell was bargained, look at the top " +
        "eight cards of your library instead. Put two of them into your hand and the rest on the " +
        "bottom of your library in a random order."

    bargain()

    spell {
        effect = Patterns.Library.lookAtTopAndKeep(
            count = DynamicAmount.Conditional(
                condition = Conditions.WasBargained,
                ifTrue = DynamicAmount.Fixed(8),
                ifFalse = DynamicAmount.Fixed(4),
            ),
            keepCount = DynamicAmount.Fixed(2),
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.Random,
            selectedLabel = "Put into hand",
            remainderLabel = "Put on bottom",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c958257e-fa70-4fa3-90a1-0497967abef3.jpg?1783915120"

        ruling(
            "2023-09-01",
            "You still put only two of the cards into your hand if you bargain this spell."
        )
        ruling(
            "2023-09-01",
            "You may sacrifice only one artifact, enchantment, or token to pay a spell's bargain cost."
        )
        ruling(
            "2023-09-01",
            "If you copy a bargained spell, the copy is also bargained."
        )
    }
}
