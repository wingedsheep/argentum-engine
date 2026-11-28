package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Dream Twist
 * {U}
 * Instant
 * Target player mills three cards.
 * Flashback {1}{U} (You may cast this card from your graveyard for its flashback cost. Then exile it.)
 */
val DreamTwist = card("Dream Twist") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target player mills three cards.\nFlashback {1}{U} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"
    spell {
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(3, player)
    }
    keywordAbility(KeywordAbility.flashback("{1}{U}"))
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5dd8790-bfdf-427d-8e8d-a5c3a64a3063.jpg"
    }
}
