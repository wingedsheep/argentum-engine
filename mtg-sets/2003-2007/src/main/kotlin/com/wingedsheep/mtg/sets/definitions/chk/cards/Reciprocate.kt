package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reciprocate
 * {W}
 * Instant
 * Exile target creature that dealt damage to you this turn.
 */
val Reciprocate = card("Reciprocate") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature that dealt damage to you this turn."

    spell {
        val t = target(TargetFilter.Creature.dealtDamageToSourceControllerThisTurn())
        effect = Effects.Exile(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Pat Lee"
        flavorText = "\"Just as the noble soul calls virtue to itself, the evil soul summons harm.\"\n" +
            "—Teachings of Eight-and-a-Half-Tails"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6c835f8-9269-4950-95c9-125d58b7b19e.jpg?1783944333"
    }
}
