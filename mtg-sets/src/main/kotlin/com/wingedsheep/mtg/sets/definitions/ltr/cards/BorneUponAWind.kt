package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Borne Upon a Wind
 * {1}{U}
 * Instant
 *
 * You may cast spells this turn as though they had flash.
 * Draw a card.
 *
 * Granting flash to every spell the controller casts for the rest of the turn (CR 702.8a)
 * is modelled via [com.wingedsheep.sdk.scripting.effects.GrantFlashToSpellsEffect], which
 * stamps a turn-scoped `FlashGrantsThisTurnComponent` on the controller. The component is
 * cleared by `CleanupPhaseManager.cleanupEndOfTurn` (CR 514.2).
 */
val BorneUponAWind = card("Borne Upon a Wind") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "You may cast spells this turn as though they had flash.\nDraw a card."

    spell {
        effect = Effects.GrantFlashToSpells(
            spellFilter = GameObjectFilter.Any,
            duration = Duration.EndOfTurn
        ).then(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Alexander Mokhov"
        flavorText = "\"I looked on Aragorn and thought how great and terrible a Lord he might " +
            "have become, had he taken the Ring for himself. Not for naught does Mordor fear him.\"" +
            "\n—Legolas"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9379675-1a32-4e2b-8aaf-5f908c595f31.jpg?1686968037"
    }
}
