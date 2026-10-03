package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Dreamtide Whale
 * {2}{U}
 * Creature — Whale
 * 7/5
 * Vanishing 2
 * Whenever a player casts their second spell each turn, proliferate.
 *
 * "A player" is `Triggers.anyPlayer.castsNth(2)`: the ordinal is counted per caster
 * (`playerSpellsCastThisTurn[casterId]`), so each player's own second spell fires it once.
 */
val DreamtideWhale = card("Dreamtide Whale") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Whale"
    power = 7
    toughness = 5
    oracleText = "Vanishing 2 (This creature enters with two time counters on it. At the beginning of your upkeep, remove a time counter from it. When the last is removed, sacrifice it.)\n" +
        "Whenever a player casts their second spell each turn, proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    keywordAbility(KeywordAbility.vanishing(2))

    triggeredAbility {
        trigger = Triggers.anyPlayer.castsNth(2)
        effect = Effects.Proliferate()
        description = "Whenever a player casts their second spell each turn, proliferate."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "59"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd7dc03e-2a61-482a-b7eb-6cdb7f375fd8.jpg?1783911291"
    }
}
