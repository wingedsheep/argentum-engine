package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Test of Loyalty — Star Trek #134
 * {B} · Sorcery
 *
 * Choose one —
 * • Plain, Simple Garak — Exile target card from a graveyard. Draw a card.
 * • Of the Obsidian Order — Return target creature card from your graveyard to your hand.
 *
 * Choosing the mode is facing a dilemma, which the engine raises for any modal spell — no extra
 * wiring needed here.
 */
val TestOfLoyalty = card("Test of Loyalty") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Plain, Simple Garak — Exile target card from a graveyard. Draw a card.\n" +
        "• Of the Obsidian Order — Return target creature card from your graveyard to your hand."

    spell {
        modal(chooseCount = 1) {
            mode("Plain, Simple Garak — Exile target card from a graveyard. Draw a card") {
                val card = target(TargetFilter.CardInGraveyard)
                effect = Effects.Move(card, Zone.EXILE) then Effects.DrawCards(1)
            }

            mode("Of the Obsidian Order — Return target creature card from your graveyard to your hand") {
                val creature = target(TargetFilter.CreatureInYourGraveyard)
                effect = Effects.ReturnToHandFromGraveyard(creature)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Bartek Fedyczak"
        flavorText = "\"Lately I've noticed that everyone seems to trust me. It's quite unnerving.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/2/722eeb21-be3c-4ae6-abd2-c3281d53c035.jpg?1792753889"
    }
}
