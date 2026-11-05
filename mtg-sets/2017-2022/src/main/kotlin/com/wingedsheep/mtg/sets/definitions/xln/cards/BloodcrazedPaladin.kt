package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Bloodcrazed Paladin
 * {1}{B}
 * Creature — Vampire Knight
 * 1/1
 * Flash
 * This creature enters with a +1/+1 counter on it for each creature that died this turn.
 *
 * "Each creature that died this turn" is controller-agnostic, so the counter count reads
 * [Player.Each] (creatures that died under any player's control this turn).
 */
val BloodcrazedPaladin = card("Bloodcrazed Paladin") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Knight"
    power = 1
    toughness = 1
    oracleText = "Flash\n" +
        "This creature enters with a +1/+1 counter on it for each creature that died this turn."

    keywords(Keyword.FLASH)

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmounts.creaturesDiedThisTurn(Player.Each)
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e2160e9-14a0-4c75-b7b0-fb09051ddb32.jpg?1783935766"
    }
}
