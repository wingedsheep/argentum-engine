package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Countervailing Winds
 * {2}{U}
 * Instant
 * Counter target spell unless its controller pays {1} for each card in your graveyard.
 * Cycling {2}
 *
 * The tax is counted on resolution from the graveyard of Countervailing Winds' controller;
 * Countervailing Winds itself is still on the stack then, so it never counts itself.
 */
val CountervailingWinds = card("Countervailing Winds") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {1} for each card in your graveyard.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessDynamicPays(DynamicAmounts.cardsInYourGraveyard())
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Tyler Jacobson"
        flavorText = "The Eternals had long since mastered every technique taught during the trials."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de1c0ef3-b32c-403a-93cb-29cf05795711.jpg?1783936054"
    }
}
