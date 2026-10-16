package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vault 21: House Gambit (PIP #69)
 * {1}{R} · Enchantment — Saga
 *
 * I, II — Discard a card, then draw a card.
 * III — Reveal up to five nonland cards from your hand. For each of those cards that has the same
 *       mana value as another card revealed this way, create a Treasure token.
 *
 * - Chapters I and II discard then draw unconditionally ("then", not "if you do").
 * - Chapter III: choose up to five nonland cards in hand and reveal them. Each revealed card is
 *   visited in turn; it earns a Treasure when at least two revealed cards (itself and another) share
 *   its mana value. Two cards at MV 2 and three at MV 3 make five Treasures; a lone MV makes none.
 *   A card's mana value in hand counts {X} as 0 (2024-03-08 ruling).
 */
val Vault21HouseGambit = card("Vault 21: House Gambit") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Discard a card, then draw a card.\n" +
        "III — Reveal up to five nonland cards from your hand. For each of those cards that has the " +
        "same mana value as another card revealed this way, create a Treasure token."

    val loot = Patterns.Hand.discardCards(1) then Effects.DrawCards(1)

    sagaChapter(1) {
        effect = loot
    }

    sagaChapter(2) {
        effect = loot
    }

    sagaChapter(3) {
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, filter = GameObjectFilter.Nonland))
            val revealed = chooseUpTo(
                5,
                from = hand,
                prompt = "Reveal up to five nonland cards from your hand",
                selectedLabel = "Reveal",
            )
            reveal(revealed, fromZone = Zone.HAND)
            run(Effects.ForEachInCollection(revealed, Effects.Pipeline {
                ifNotEmpty(
                    revealed,
                    filter = GameObjectFilter.Any.sharingManaValueWith(EffectTarget.IterationEntity),
                    minSize = 2,
                ) {
                    run(Effects.CreateTreasure())
                }
            }))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "69"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40972644-a284-4c7c-a451-d70eff7488ab.jpg?1783912400"
        ruling("2024-03-08", "If a card in your hand has {X} in its mana cost, X is 0.")
    }
}
