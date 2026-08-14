package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry

/**
 * Crimson Operative — Marvel Super Heroes #126 (common)
 * {3}{R} · Artifact Creature — Human Villain · 3/2
 *
 * Prowess
 * When this creature enters, exile the top card of your library. Until the end of your next turn,
 * you may play that card.
 *
 * The enters ability is the named impulse-draw pattern ([Patterns.Exile.impulse]) with the
 * longer [MayPlayExpiry.UntilEndOfNextTurn] window — gather the top card, move it to exile, and
 * grant a may-play-from-exile permission on that collection (Alania's Pathmaker's shape, expressed
 * through the shared recipe). The exiled card still pays its costs and follows normal timing.
 */
val CrimsonOperative = card("Crimson Operative") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Human Villain"
    power = 3
    toughness = 2
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until " +
        "end of turn.)\n" +
        "When this creature enters, exile the top card of your library. Until the end of your " +
        "next turn, you may play that card."

    prowess()

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Exile.impulse(
            count = 1,
            expiry = MayPlayExpiry.UntilEndOfNextTurn,
            storeAs = "crimsonOperativeExiled"
        )
        description = "When this creature enters, exile the top card of your library. Until the " +
            "end of your next turn, you may play that card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Kevin Glint"
        flavorText = "\"I think you will find the latest version of my armor particularly " +
            "impressive.\"\n—Crimson Dynamo, Dimitri Bukharin"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4bc077c-f220-47a5-aaf4-5324ca23d0c5.jpg?1783902933"
    }
}
