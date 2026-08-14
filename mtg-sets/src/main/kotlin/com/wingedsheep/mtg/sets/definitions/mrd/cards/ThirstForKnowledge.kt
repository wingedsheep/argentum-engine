package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Thirst for Knowledge
 * {2}{U}
 * Instant
 *
 * Draw three cards. Then discard two cards unless you discard an artifact card.
 *
 * The discard instruction is one selection: a single artifact card satisfies it, otherwise two
 * cards must be selected.
 */
val ThirstForKnowledge = card("Thirst for Knowledge") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw three cards. Then discard two cards unless you discard an artifact card."

    spell {
        effect = Effects.DrawCards(3)
            .then(Effects.DiscardUnlessMatching(2, GameObjectFilter.Artifact))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Ben Thompson"
        flavorText = "Lymph, the fluid essence of blinkmoths, is prized by wizards for the rush of intellect it provides."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0ff1f608-203e-4413-8753-37fc49731c87.jpg?1783944550"
    }
}
