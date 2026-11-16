package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Mystic Meditation
 * {3}{U}
 * Sorcery
 *
 * Draw three cards. Then discard two cards unless you discard a creature card.
 *
 * Thirst for Knowledge's shape: one choose-two selection whose minimum drops to one once it holds a
 * creature card — so either a single creature card or any two cards may be discarded (ruling).
 */
val MysticMeditation = card("Mystic Meditation") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw three cards. Then discard two cards unless you discard a creature card."

    spell {
        effect = Effects.DrawCards(3) then Effects.DiscardUnlessMatching(
            count = 2,
            unlessFilter = GameObjectFilter.Creature,
            prompt = "Discard two cards, or a single creature card",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "Howard Lyon"
        flavorText = "\"Still the mind and quiet the heart. Only then will you hear the Multiverse's great truths.\"\n—Narset"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d169f469-10d5-4ae8-88fe-12a8e20ab01b.jpg?1783938606"
        ruling("2015-02-25", "You may choose to discard a creature card and another card if you want to.")
    }
}
