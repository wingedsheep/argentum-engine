package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Viral Spawning
 * {2}{G}
 * Sorcery
 * Create a 3/3 green Phyrexian Beast creature token with toxic 1.
 * Corrupted — As long as an opponent has three or more poison counters and this card is in your
 * graveyard, it has flashback {2}{G}.
 *
 * The corrupted flashback is a printed flashback gated by `Conditions.Corrupted`: castable from
 * the graveyard only while the condition holds, and — per the flashback ruling — still exiled
 * once cast that way even if the condition lapses before it leaves the stack.
 */
val ViralSpawning = card("Viral Spawning") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Create a 3/3 green Phyrexian Beast creature token with toxic 1. (Players dealt combat damage by it also get a poison counter.)\n" +
        "Corrupted — As long as an opponent has three or more poison counters and this card is in your graveyard, it has flashback {2}{G}. (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Phyrexian", "Beast"),
            numericKeywords = listOf(KeywordAbility.toxic(1)),
        )
    }

    keywordAbility(KeywordAbility.flashback("{2}{G}", Conditions.Corrupted))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Denis Zhbankov"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85ad30a1-3ecc-42ca-afe8-85df5bad9196.jpg?1783918006"
    }
}
