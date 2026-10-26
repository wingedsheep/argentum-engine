package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CounterPlacementScale
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ScaleCounterPlacement
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Vorinclex, Monstrous Raider — Kaldheim #199 (canonical printing)
 * {4}{G}{G} Legendary Creature — Phyrexian Praetor 6/6
 *
 * Both clauses key on *who* puts the counters (the ruling): `placer = Player.You` doubles, and
 * `placer = Player.EachOpponent` halves, rounded down. Every kind, on a permanent or a player.
 */
val VorinclexMonstrousRaider = card("Vorinclex, Monstrous Raider") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Phyrexian Praetor"
    oracleText = "Trample, haste\n" +
        "If you would put one or more counters on a permanent or player, put twice that many of each of those kinds of counters on that permanent or player instead.\n" +
        "If an opponent would put one or more counters on a permanent or player, they put half that many of each of those kinds of counters on that permanent or player instead, rounded down."
    power = 6
    toughness = 6
    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    replacementEffect(
        ScaleCounterPlacement(
            placer = Player.You,
            appliesTo = EventPattern.CounterPlacementEvent(counterType = null, recipient = Recipient.Any)
        )
    )
    replacementEffect(
        ScaleCounterPlacement(
            scale = CounterPlacementScale.HALF_ROUNDED_DOWN,
            placer = Player.EachOpponent,
            appliesTo = EventPattern.CounterPlacementEvent(counterType = null, recipient = Recipient.Any)
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "199"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92613468-205e-488b-930d-11908477e9f8.jpg"
    }
}
