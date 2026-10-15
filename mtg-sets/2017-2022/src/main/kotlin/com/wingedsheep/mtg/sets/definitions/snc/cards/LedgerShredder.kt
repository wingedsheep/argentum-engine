package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ledger Shredder
 * {1}{U}
 * Creature — Bird Advisor
 * 1 / 3
 * Flying
 * Whenever a player casts their second spell each turn, this creature connives.
 */
val LedgerShredder = card("Ledger Shredder") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Advisor"
    oracleText = "Flying\n" +
        "Whenever a player casts their second spell each turn, this creature connives. " +
        "(Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)"
    power = 1
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anyPlayer.castsNth(2)
        effect = Effects.Connive()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Mila Pesic"
        flavorText = "No evidence, no crime."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7ea4b5bc-18a4-45db-a56a-ab3f8bd2fb0d.jpg"
    }
}
