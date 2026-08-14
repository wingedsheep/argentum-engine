package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Brave the Wilds
 * {G}
 * Sorcery
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * If this spell was bargained, target land you control becomes a 3/3 Elemental creature with haste
 * that's still a land.
 * Search your library for a basic land card, reveal it, put it into your hand, then shuffle.
 *
 * The **bargain-only target** shape (CR 702.166d, and the ruling below): the land target exists only
 * on the bargained branch. That's the shared optional-additional-cost rail's `kickerTarget` /
 * `kickerEffect` slots — they serve whichever mechanic declared, bargain here — so an unbargained
 * Brave the Wilds has no targets at all and can be cast with an empty board, while a bargained one
 * can't be cast unless a land you control is available to target.
 *
 * Because the branch replaces the whole effect rather than riding on it, the bargained branch has to
 * restate the search. Per the ruling, that's exactly right: if the land is an illegal target on
 * resolution the *entire* spell is countered, and no search happens.
 *
 * The animation has no duration on the card, so it's [Duration.Permanent] — the land stays a 3/3
 * Elemental with haste for as long as it's on the battlefield. `BecomeCreature` adds the creature
 * type and base P/T without removing land types, which is the "that's still a land" clause. No
 * colors are set: the card doesn't make it green, so an animated Forest is still a colorless
 * permanent.
 */
val BraveTheWilds = card("Brave the Wilds") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "If this spell was bargained, target land you control becomes a 3/3 Elemental creature " +
        "with haste that's still a land.\n" +
        "Search your library for a basic land card, reveal it, put it into your hand, then shuffle."

    bargain()

    spell {
        val searchForBasic = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.HAND,
            reveal = true,
        )

        effect = searchForBasic

        val land = kickerTarget(
            "target land you control",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Land.youControl())),
        )
        kickerEffect = Effects.Composite(
            Effects.BecomeCreature(
                target = land,
                power = 3,
                toughness = 3,
                keywords = setOf(Keyword.HASTE),
                creatureTypes = setOf("Elemental"),
                duration = Duration.Permanent,
            ),
            searchForBasic,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "165"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/821b9e86-d108-42e5-b642-c5e07ab16c37.jpg?1783915084"

        ruling(
            "2023-09-01",
            "If you bargained Brave the Wilds and the target land is an illegal target by the time " +
                "it tries to resolve, the spell won't resolve. You won't search for a basic land " +
                "card, and you won't shuffle."
        )
        ruling(
            "2023-09-01",
            "Some instant and sorcery spells require additional targets if they're bargained. You " +
                "ignore those targeting requirements if those spells aren't bargained, and you " +
                "can't bargain those spells unless you can choose the appropriate targets."
        )
        ruling(
            "2023-09-01",
            "You may sacrifice only one artifact, enchantment, or token to pay a spell's bargain cost."
        )
    }
}
