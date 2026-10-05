package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Military Intelligence
 * {1}{U}
 * Enchantment
 * Whenever you attack with two or more creatures, draw a card.
 *
 * Player-level attack batch trigger: fires once per attack declaration in which you declare at
 * least two attackers ([Triggers.you] `.attacks(minAttackers = 2)`), not once per attacker.
 */
val MilitaryIntelligence = card("Military Intelligence") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you attack with two or more creatures, draw a card."

    triggeredAbility {
        trigger = Triggers.you.attacks(minAttackers = 2)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Craig J Spearing"
        flavorText = "To know the battlefield is to anticipate the enemy. To know the enemy is to anticipate victory."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f10fafb6-3476-4cf1-a762-42fec628a579.jpg?1783939190"
    }
}
