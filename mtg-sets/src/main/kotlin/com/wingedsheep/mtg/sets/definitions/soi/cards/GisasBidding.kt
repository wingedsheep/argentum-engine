package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity

/**
 * Gisa's Bidding (Shadows over Innistrad #114)
 * {2}{B}{B}
 * Sorcery
 *
 * Create two 2/2 black Zombie creature tokens.
 * Madness {2}{B}
 *
 * Madness (CR 702.35) is the only wrinkle: discarding this exiles it and offers the cast for
 * {2}{B}. Because that cast happens while the madness trigger resolves, this *sorcery* can make
 * its Zombies on an opponent's turn.
 */
val GisasBidding = card("Gisa's Bidding") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Create two 2/2 black Zombie creature tokens.\n" +
        "Madness {2}{B} (If you discard this card, discard it into exile. When you do, cast it " +
        "for its madness cost or put it into your graveyard.)"

    spell {
        effect = Effects.CreateToken(
            count = 2,
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
        )
    }

    madness("{2}{B}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Jason Felix"
        flavorText = "\"Soft dirt makes for light work.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e01e904c-7d8e-447b-90cb-1f4ae3fb304d.jpg?1783937773"
    }
}
