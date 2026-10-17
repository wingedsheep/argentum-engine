package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceDamageWithMill
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Angel of Suffering
 * {3}{B}{B}
 * Creature — Nightmare Angel
 * 5/3
 *
 * Flying
 * If damage would be dealt to you, prevent that damage and mill twice that many cards.
 *
 * A [ReplaceDamageWithMill] prevention effect with you as the miller. Per the rulings, the damage is
 * prevented even if the library is too small, and damage that can't be prevented is still dealt —
 * you mill twice that many cards either way.
 */
val AngelOfSuffering = card("Angel of Suffering") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightmare Angel"
    power = 5
    toughness = 3
    oracleText = "Flying\n" +
        "If damage would be dealt to you, prevent that damage and mill twice that many cards."

    keywords(Keyword.FLYING)

    replacementEffect(
        ReplaceDamageWithMill(
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.You),
            miller = Player.You,
            multiplier = 2,
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "67"
        artist = "Martina Fačková"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd4aee25-496d-453e-95b7-d773fe21cacc.jpg?1783923135"
    }
}
