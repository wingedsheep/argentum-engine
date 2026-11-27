package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Lair Delve — Avacyn Restored #184 (canonical printing; its only printing)
 * {2}{G} · Sorcery
 *
 * Reveal the top two cards of your library. Put all creature and land cards revealed this way into
 * your hand and the rest on the bottom of your library in any order.
 *
 * Beast Hunt's shape with a creature-or-land filter; the rest keep the pattern's bottom-of-library
 * destination, ordered by the controller ("in any order").
 */
val LairDelve = card("Lair Delve") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Reveal the top two cards of your library. Put all creature and land cards revealed this way " +
        "into your hand and the rest on the bottom of your library in any order."

    spell {
        effect = Patterns.Library.revealTopPutAllMatchingToHand(
            count = 2,
            filter = GameObjectFilter.Creature or GameObjectFilter.Land,
            restOrder = CardOrder.ControllerChooses,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "Jason A. Engle"
        flavorText = "\"The fiends have fled. Well, good riddance. Nothing I like more than an empty lair.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/0/604948d8-6224-45ca-9ebb-d716644bbfd0.jpg?1783940664"
    }
}
