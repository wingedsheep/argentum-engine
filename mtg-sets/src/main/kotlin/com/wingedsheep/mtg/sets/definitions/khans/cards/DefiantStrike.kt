package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Defiant Strike
 * {W}
 * Instant
 * Target creature gets +1/+0 until end of turn. Draw a card.
 */
val DefiantStrike = card("Defiant Strike") {
    manaCost = "{W}"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+0 until end of turn. Draw a card."

    spell {
        val t = target("target", TargetCreature())
        effect = Effects.ModifyStats(1, 0, t)
            .then(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Anastasia Ovchinnikova"
        flavorText = "\"Stand where the whole battle can see you. Strike so they'll never forget.\" —Anafenza, khan of the Abzan"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39ec83fa-5cb4-4633-8827-36d874a2d2e7.jpg?1562785005"
    }
}
