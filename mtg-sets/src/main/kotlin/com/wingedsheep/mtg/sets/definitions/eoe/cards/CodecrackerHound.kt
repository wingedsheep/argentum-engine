package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Codecracker Hound
 * {2}{U}
 * Creature — Dog
 * When this creature enters, look at the top two cards of your library. Put one into your hand
 * and the other into your graveyard.
 * Warp {2}{U}
 * 2/1
 *
 * lookAtTopAndKeep with count=2, keepCount=1 — defaults send the kept card to HAND and the
 * remainder to GRAVEYARD, matching the oracle exactly.
 */
val CodecrackerHound = card("Codecracker Hound") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Dog"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, look at the top two cards of your library. Put one into your hand and the other into your graveyard.\n" +
        "Warp {2}{U} (You may cast this card from your hand for its warp cost. Exile this creature at the beginning of the next end step, then you may cast it from exile on a later turn.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.lookAtTopAndKeep(count = 2, keepCount = 1)
    }

    warp = "{2}{U}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "50"
        artist = "Julia Metzger"
        flavorText = "Even across the Edge, a dog is still a dog."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6723b891-6013-4ec6-b439-2233d270dc48.jpg?1752946747"
        ruling("2025-07-25", "If there's only one card in your library as Codecracker Hound's first ability resolves, you'll put that card into your hand.")
    }
}
