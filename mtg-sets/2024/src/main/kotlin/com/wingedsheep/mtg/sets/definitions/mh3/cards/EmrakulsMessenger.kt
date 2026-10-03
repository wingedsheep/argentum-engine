package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Emrakul's Messenger — Modern Horizons 3 #61 (uncommon)
 * {1}{U} · Creature — Eldrazi Faerie Rogue · 2/1
 *
 * Devoid
 * Flying
 * Whenever you draw your second card each turn, create a 0/1 colorless Eldrazi Spawn creature
 * token with "Sacrifice this token: Add {C}."
 */
val EmrakulsMessenger = card("Emrakul's Messenger") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Faerie Rogue"
    power = 2
    toughness = 1
    oracleText = "Devoid (This card has no color.)\n" +
        "Flying\n" +
        "Whenever you draw your second card each turn, create a 0/1 colorless Eldrazi Spawn creature " +
        "token with \"Sacrifice this token: Add {C}.\""

    keywords(Keyword.DEVOID, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        effect = Effects.CreateEldraziSpawn()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "David Álvarez"
        flavorText = "\"Obey her will! Don't be a fool! Submit your mind to Emrakul!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0f818d7-320c-47b9-a083-40007f3d91ea.jpg?1783911291"
    }
}
