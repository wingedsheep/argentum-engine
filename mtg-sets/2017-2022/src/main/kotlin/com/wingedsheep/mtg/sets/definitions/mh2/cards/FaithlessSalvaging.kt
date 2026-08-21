package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Faithless Salvaging — Modern Horizons 2 #122
 * {1}{R} · Instant
 *
 * Discard a card, then draw a card.
 * Rebound (If you cast this spell from your hand, exile it as it resolves. At the beginning of your next upkeep, you may cast this card from exile without paying its mana cost.)
 *
 * The "then" is load-bearing: the discard fully resolves before the draw, so the card you discard
 * can never be the one you draw. [Patterns.Hand.discardCards] is the Gather → Select → Move
 * pipeline, and it is kept as its own step inside [Effects.Composite] rather than chained with
 * `then` — `then` flattens a composite receiver, which would dissolve the discard pipeline into
 * the outer sequence.
 *
 * Like Terramorph in this same set, [Keyword.REBOUND] has a real consumer — `StackResolver` reads
 * it off `cardDef.keywords` as the spell resolves — so the bare keyword is the whole rebound
 * implementation; no exile-and-delayed-trigger wiring is needed here.
 */
val FaithlessSalvaging = card("Faithless Salvaging") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Discard a card, then draw a card.\n" +
        "Rebound (If you cast this spell from your hand, exile it as it resolves. At the beginning of your next upkeep, you may cast this card from exile without paying its mana cost.)"

    keywords(Keyword.REBOUND)

    spell {
        effect = Effects.Composite(
            Patterns.Hand.discardCards(1),
            Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Bud Cook"
        flavorText = "All that's left are broken beliefs."
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6ed4077-4ed5-48fb-91c9-a9195d652978.jpg?1783926845"
    }
}
