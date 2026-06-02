package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Ravenous Rats
 * {1}{B}
 * Creature — Rat
 * 1/1
 * When this creature enters, target opponent discards a card.
 *
 * Portal Second Age is the card's earliest real-expansion printing, so the canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives here. Later sets (Invasion, etc.)
 * contribute reprint [com.wingedsheep.sdk.model.Printing] rows.
 */
val RavenousRats = card("Ravenous Rats") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, target opponent discards a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target opponent", TargetOpponent())
        effect = HandPatterns.discardCards(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Edward P. Beard, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/8899244b-737a-43a9-9241-15a650b47bed.jpg?1562927249"
    }
}
