package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DrawCardsEffect

/**
 * Enchantress's Presence
 * {2}{G}
 * Enchantment
 * Whenever you cast an enchantment spell, draw a card.
 */
val EnchantresssPresence = card("Enchantress's Presence") {
    manaCost = "{2}{G}"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast an enchantment spell, draw a card."

    triggeredAbility {
        trigger = Triggers.YouCastEnchantment
        effect = DrawCardsEffect(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "261"
        artist = "Rebecca Guay"
        flavorText = "\"The wise learn from successes as well as mistakes.\""
        imageUri = "https://cards.scryfall.io/large/front/7/5/75def198-99d6-4b0a-8878-5151f44bc0a4.jpg?1562922860"
    }
}
