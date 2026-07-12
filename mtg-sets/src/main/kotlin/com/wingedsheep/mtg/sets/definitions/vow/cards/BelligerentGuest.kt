package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Belligerent Guest
 * {2}{R}
 * Creature — Vampire
 * 3/2
 *
 * Trample
 * Whenever this creature deals combat damage to a player, create a Blood token.
 */
val BelligerentGuest = card("Belligerent Guest") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire"
    power = 3
    toughness = 2
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker " +
        "it's attacking.)\n" +
        "Whenever this creature deals combat damage to a player, create a Blood token. (It's an " +
        "artifact with \"{1}, {T}, Discard a card, Sacrifice this token: Draw a card.\")"

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.CreateBlood(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "301"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/924b1c15-811a-4d11-a481-f904002a6740.jpg?1782702983"
    }
}
