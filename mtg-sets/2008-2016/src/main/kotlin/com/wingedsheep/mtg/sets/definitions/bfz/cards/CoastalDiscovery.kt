package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Coastal Discovery
 * {3}{U}
 * Sorcery
 * Draw two cards.
 * Awaken 4—{5}{U} (If you cast this spell for {5}{U}, also put four +1/+1 counters on target land
 * you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)
 *
 * Cast for its awaken cost, the land is the spell's only target: if it's gone by resolution the
 * whole spell doesn't resolve, so no cards are drawn (CR 608.2b).
 */
val CoastalDiscovery = card("Coastal Discovery") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw two cards.\n" +
        "Awaken 4—{5}{U} (If you cast this spell for {5}{U}, also put four +1/+1 counters on target " +
        "land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)"

    keywordAbility(KeywordAbility.awaken(4, "{5}{U}"))

    spell {
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1df2411-0b0d-4b93-bf30-a22487b3c6ef.jpg?1783938209"
    }
}
