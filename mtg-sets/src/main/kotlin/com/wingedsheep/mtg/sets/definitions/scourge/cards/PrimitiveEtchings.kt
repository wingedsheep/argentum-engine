package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RevealFirstDrawEachTurn

/**
 * Primitive Etchings
 * {2}{G}{G}
 * Enchantment
 * Reveal the first card you draw each turn. Whenever you reveal a creature
 * card this way, draw a card.
 */
val PrimitiveEtchings = card("Primitive Etchings") {
    manaCost = "{2}{G}{G}"
    typeLine = "Enchantment"
    oracleText = "Reveal the first card you draw each turn. Whenever you reveal a creature card this way, draw a card."

    staticAbility {
        ability = RevealFirstDrawEachTurn
    }

    triggeredAbility {
        trigger = Triggers.RevealCreatureFromDraw
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "David Martin"
        flavorText = "Ancient carvings in the trees of Krosa glow with power once again."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/eae26b8d-c3af-42d1-94f4-56950ceac1c7.jpg?1562536644"
    }
}
