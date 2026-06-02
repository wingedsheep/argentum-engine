package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Abomination of Gudul
 * {3}{B}{G}{U}
 * Creature — Horror
 * 3/4
 * Flying
 * Whenever Abomination of Gudul deals combat damage to a player, you may draw a card. If you do, discard a card.
 * Morph {2}{B}{G}{U}
 */
val AbominationOfGudul = card("Abomination of Gudul") {
    manaCost = "{3}{B}{G}{U}"
    colorIdentity = "UBG"
    typeLine = "Creature — Horror"
    power = 3
    toughness = 4
    oracleText = "Flying\nWhenever Abomination of Gudul deals combat damage to a player, you may draw a card. If you do, discard a card.\nMorph {2}{B}{G}{U} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = MayEffect(HandPatterns.loot())
    }

    morph = "{2}{B}{G}{U}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "159"
        artist = "Erica Yang"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7df9759e-1072-4a6a-be57-f73b15bf3847.jpg?1562789157"
    }
}
