package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Hymn to Tourach
 * {B}{B}
 * Sorcery
 * Target player discards two cards at random.
 *
 * Both cards are chosen at random from the hand *as the spell resolves*, one after the other, so
 * a hand of one card discards that card and nothing more.
 */
val HymnToTourach = card("Hymn to Tourach") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player discards two cards at random."

    spell {
        val t = target("target player", TargetPlayer())
        effect = Patterns.Hand.discardRandom(2, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38a"
        artist = "Susan Van Camp"
        flavorText = "\"The eerie, wailing Hymn caused insanity even in hardened warriors.\"\n—*Sarpadian Empires, vol. II*"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb9273ea-9a41-42e3-8c9c-0d50b127a818.jpg?1783947903"
    }
}
