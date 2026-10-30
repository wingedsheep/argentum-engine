package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Improbable Alliance
 * {U}{R}
 * Enchantment
 *
 * Whenever you draw your second card each turn, create a 1/1 blue Faerie creature token with flying.
 * {4}{U}{R}: Draw a card, then discard a card.
 */
val ImprobableAlliance = card("Improbable Alliance") {
    manaCost = "{U}{R}"
    colorIdentity = "RU"
    typeLine = "Enchantment"
    oracleText = "Whenever you draw your second card each turn, create a 1/1 blue Faerie creature token with flying.\n" +
        "{4}{U}{R}: Draw a card, then discard a card."

    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Faerie"),
            keywords = setOf(Keyword.FLYING),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{4}{U}{R}")
        effect = Patterns.Hand.loot()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "Zoltan Boros"
        flavorText = "\"I agreed to light his way as long as he always had my back.\"\n—Squill, Mistford pixie"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/0461867b-ec35-4d37-a398-5247e06c4afe.jpg?1783932596"
    }
}
