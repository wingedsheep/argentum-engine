package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Thrilling Discovery — Strixhaven: School of Mages #243 (canonical printing)
 * {R}{W} · Sorcery
 *
 * You gain 2 life. Then you may discard two cards. If you do, draw three cards.
 *
 * "You may discard two cards" is only an option with at least two cards in hand — a player with
 * one card can't discard two, so can't do it and draws nothing. The offer is therefore gated on
 * [Conditions.CardsInHandAtLeast] (2), read after the life gain, and Keldon Raider's
 * `May(discard then draw)` runs inside it: with two or more cards the discard always discards
 * exactly two, so the draw follows only a full discard.
 */
val ThrillingDiscovery = card("Thrilling Discovery") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Sorcery"
    oracleText = "You gain 2 life. Then you may discard two cards. If you do, draw three cards."

    spell {
        effect = Effects.GainLife(2) then Effects.If(
            condition = Conditions.CardsInHandAtLeast(2),
            then = Effects.May(
                Effects.Discard(2) then Effects.DrawCards(3),
                descriptionOverride = "You may discard two cards. If you do, draw three cards."
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "243"
        artist = "Campbell White"
        flavorText = "\"This must be the lost city of Zantafar, where my people lived before " +
            "becoming nomads. Think of the history we'll learn here!\"\n—Quintorius, Lorehold mage-student"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/bac1f45e-1884-490e-a94f-f7d312f0e229.jpg?1783927286"
    }
}
