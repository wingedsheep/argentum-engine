package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Thassa's Bounty
 * {5}{U}
 * Sorcery
 * Draw three cards. Target player mills three cards.
 */
val ThassasBounty = card("Thassa's Bounty") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw three cards. Target player mills three cards."

    spell {
        val player = target(Targets.Player)
        effect = Effects.DrawCards(3) then Patterns.Library.mill(3, player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Ryan Yee"
        flavorText = "\"Was this gift cast adrift for any to find, or did Thassa guide the currents to bring it " +
            "to me alone?\"\n—Kenessos, priest of Thassa"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3ac987ac-3f81-4704-b42c-651f44670641.jpg?1783939788"
    }
}
