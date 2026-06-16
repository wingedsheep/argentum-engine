package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Winternight Stories — Tarkir: Dragonstorm #67
 * {2}{U} · Sorcery · Rare
 *
 * Draw three cards. Then discard two cards unless you discard a creature card.
 * Harmonize {4}{U}
 *
 * The discard instruction is one selection: a single creature card satisfies it, otherwise two
 * cards must be selected.
 */
val WinternightStories = card("Winternight Stories") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw three cards. Then discard two cards unless you discard a creature card.\n" +
        "Harmonize {4}{U} (You may cast this card from your graveyard for its harmonize cost. " +
        "You may tap a creature you control to reduce that cost by {X}, where X is its power. " +
        "Then exile this spell.)"

    spell {
        effect = Effects.DrawCards(3)
            .then(Effects.DiscardUnlessMatching(2, GameObjectFilter.Creature))
    }

    keywordAbility(KeywordAbility.harmonize("{4}{U}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Zara Alfonso"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64d9367c-f50c-4568-aa63-6760c44ecaeb.jpg?1743204229"
    }
}
