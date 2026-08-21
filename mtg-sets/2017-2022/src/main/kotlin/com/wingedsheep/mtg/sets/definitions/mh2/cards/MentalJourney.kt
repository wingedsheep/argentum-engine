package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Mental Journey — Modern Horizons 2 #51
 * {4}{U}{U} · Instant
 *
 * Draw three cards.
 * Basic landcycling {1}{U} ({1}{U}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)
 *
 * A plain draw spell whose real text is the escape hatch: [KeywordAbility.basicLandcycling] narrows
 * the shared typecycling search to *basic* land cards, so an expensive card in a mana-light opening
 * hand converts into a land instead. The cycling ability is a discard-cost activated ability that
 * functions only from hand (CR 702.29a), which is why it is a `keywordAbility` on the card rather
 * than part of the spell script.
 */
val MentalJourney = card("Mental Journey") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw three cards.\n" +
        "Basic landcycling {1}{U} ({1}{U}, Discard this card: Search your library for a basic land card, reveal it, put it into your hand, then shuffle.)"

    spell {
        effect = Effects.DrawCards(3)
    }

    keywordAbility(KeywordAbility.basicLandcycling("{1}{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Jim Pavelec"
        flavorText = "\"My wandering mind has always been an asset.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ec38124-46ae-4a38-aab2-8a73cc22b1ef.jpg?1783926876"
    }
}
