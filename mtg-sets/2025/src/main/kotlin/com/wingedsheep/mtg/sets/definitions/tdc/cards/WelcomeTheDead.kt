package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Welcome the Dead — Tarkir: Dragonstorm Commander #30 (canonical printing)
 * {3}{B} · Sorcery
 *
 * Draw two cards, then discard a card and you lose 2 life. Create X tapped 2/2 black Zombie Druid
 * creature tokens, where X is the number of cards that were put into your graveyard from your hand
 * or library this turn.
 * Flashback {5}{B}
 *
 * X is turn history summed over two origins — the hand and library halves of the per-origin
 * graveyard tracker — read as the tokens are created, so the card discarded by this spell counts.
 * Welcome the Dead itself goes to the graveyard from the stack (or is exiled by flashback) only
 * after it resolves, so it never counts toward its own X.
 */
val WelcomeTheDead = card("Welcome the Dead") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Draw two cards, then discard a card and you lose 2 life. Create X tapped 2/2 black Zombie " +
        "Druid creature tokens, where X is the number of cards that were put into your graveyard from your " +
        "hand or library this turn.\n" +
        "Flashback {5}{B} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Effects.DrawCards(2) then
            Effects.Discard(1) then
            Effects.LoseLife(2, EffectTarget.Controller) then
            Effects.CreateToken(
                count = DynamicAmounts.cardsPutIntoGraveyardFromHandThisTurn() +
                    DynamicAmounts.cardsPutIntoGraveyardFromLibraryThisTurn(),
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie", "Druid"),
                name = "Zombie Druid",
                tapped = true,
                imageUri = "https://cards.scryfall.io/normal/front/f/1/f10d5813-7818-43e8-b08d-4ed8c54d0366.jpg?1783906785",
            )
    }
    keywordAbility(KeywordAbility.flashback("{5}{B}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Ioannis Fiore"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f472ea5c-191e-4b19-8ea7-f8649af91530.jpg?1783907174"
    }
}
