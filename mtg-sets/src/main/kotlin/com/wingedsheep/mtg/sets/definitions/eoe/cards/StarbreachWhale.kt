package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Starbreach Whale
 * {4}{U}
 * Creature — Whale
 * Flying
 * When this creature enters, surveil 2. (Look at the top two cards of your library, then put any number of them into your graveyard and the rest on top of your library in any order.)
 * Warp {1}{U} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)
 * 3/5
 */
val StarbreachWhale = card("Starbreach Whale") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Whale"
    oracleText = "Flying\nWhen this creature enters, surveil 2. (Look at the top two cards of your library, then put any number of them into your graveyard and the rest on top of your library in any order.)\n" +
        "Warp {1}{U} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)"
    power = 3
    toughness = 5

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.surveil(2)
    }

    warp = "{1}{U}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a1a0476-7145-4493-97e5-4fc05c85e476.jpg?1752946862"
    }
}
