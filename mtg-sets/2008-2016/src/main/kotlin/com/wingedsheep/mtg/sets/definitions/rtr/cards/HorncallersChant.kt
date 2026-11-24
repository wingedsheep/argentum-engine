package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Horncaller's Chant
 * {7}{G}
 * Sorcery
 *
 * Create a 4/4 green Rhino creature token with trample, then populate. (Create a token that's a
 * copy of a creature token you control.)
 *
 * Same shape as Coursers' Accord: [Effects.CreateToken] then populate via
 * [Effects.CreateTokenCopyOfChosenPermanent] over creature tokens you control (chosen, not targeted).
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 */
val HorncallersChant = card("Horncaller's Chant") {
    manaCost = "{7}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Create a 4/4 green Rhino creature token with trample, then populate. (Create a token " +
        "that's a copy of a creature token you control.)"

    spell {
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Rhino"),
            keywords = setOf(Keyword.TRAMPLE),
        ) then Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Creature.token())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "Eric Velhagen"
        flavorText = "\"The Selesnya are gathering armies. Do you still believe they aren't preparing for war?\"\n—Teysa, to Isperia"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b8d33ed-9ca2-41d1-ba35-fdeb5b88ad44.jpg?1783940348"
    }
}
