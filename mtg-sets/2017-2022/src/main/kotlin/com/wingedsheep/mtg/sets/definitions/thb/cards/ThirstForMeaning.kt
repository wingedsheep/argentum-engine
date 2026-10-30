package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Thirst for Meaning
 * {2}{U}
 * Instant
 * Draw three cards. Then discard two cards unless you discard an enchantment card.
 *
 * Thirst for Knowledge with an enchantment in place of the artifact: one selection, satisfied by a
 * single enchantment card or by any two cards (ruling).
 */
val ThirstForMeaning = card("Thirst for Meaning") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw three cards. Then discard two cards unless you discard an enchantment card."

    spell {
        effect = Effects.DrawCards(3) then Effects.DiscardUnlessMatching(2, GameObjectFilter.Enchantment)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Brian Valeza"
        flavorText = "A sip of recollection, an ocean of regrets."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/2422973d-36ee-4b8c-9a47-fcd160aa9f63.jpg?1783931575"
        ruling(
            "2020-01-24",
            "You can discard either one enchantment card or two cards which may or may not be enchantments. " +
                "If you really want to, you can discard two enchantment cards."
        )
    }
}
