package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Isolation at Orthanc
 * {3}{U}
 * Instant
 * Put target creature into its owner's library second from the top.
 */
val IsolationAtOrthanc = card("Isolation at Orthanc") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Put target creature into its owner's library second from the top."

    spell {
        val creature = target("target creature", Targets.Creature)
        // 0-indexed: position 1 = second from the top.
        effect = Effects.PutIntoLibraryNthFromTop(creature, 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Erikas Perl"
        flavorText = "\"They took me and set me on the pinnacle of Orthanc. I stood alone on an island in the clouds; I had no chance of escape, and my days were bitter and cold.\"\n—Gandalf"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54b9c323-ac9b-4864-bd57-81b557f44114.jpg?1686968162"
    }
}
