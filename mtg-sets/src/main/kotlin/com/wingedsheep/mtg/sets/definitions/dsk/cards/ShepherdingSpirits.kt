package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Shepherding Spirits
 * {4}{W}{W}
 * Creature — Spirit
 * 4/5
 *
 * Flying
 * Plainscycling {2} ({2}, Discard this card: Search your library for a Plains card, reveal it,
 * put it into your hand, then shuffle.)
 *
 * Plainscycling is typecycling for the Plains subtype — modeled via
 * [KeywordAbility.typecycling] (searches for any Plains card, basic or not).
 */
val ShepherdingSpirits = card("Shepherding Spirits") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    power = 4
    toughness = 5
    oracleText = "Flying\n" +
        "Plainscycling {2} ({2}, Discard this card: Search your library for a Plains card, " +
        "reveal it, put it into your hand, then shuffle.)"

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.typecycling("Plains", ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Billy Christian"
        flavorText = "\"This way, Melinda. A hundred years haven't improved your sense of direction.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86c02ab1-0fb2-4cb0-a871-bfad406726fc.jpg?1726285971"
    }
}
