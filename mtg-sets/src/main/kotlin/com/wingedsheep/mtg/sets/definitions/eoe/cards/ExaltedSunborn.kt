package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleTokenCreation

/**
 * Exalted Sunborn
 * {3}{W}{W}
 * Creature — Angel Wizard
 * Flying, lifelink
 * If one or more tokens would be created under your control, twice that many of those tokens
 * are created instead.
 * Warp {1}{W} (You may cast this card from your hand for its warp cost. Exile this creature
 * at the beginning of the next end step, then you may cast it from exile on a later turn.)
 * 4/5
 */
val ExaltedSunborn = card("Exalted Sunborn") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Wizard"
    oracleText = "Flying, lifelink\n" +
        "If one or more tokens would be created under your control, twice that many of those tokens are created instead.\n" +
        "Warp {1}{W} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)"
    power = 4
    toughness = 5

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    // Doubling Season-style token replacement. The engine resolves the count
    // multiplier in CreateTokenExecutor before any per-token replacements run.
    replacementEffect(DoubleTokenCreation())

    warp = "{1}{W}"

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "15"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e1fe101-f634-41e5-9aa4-e8d7474535dc.jpg?1752946614"
    }
}
