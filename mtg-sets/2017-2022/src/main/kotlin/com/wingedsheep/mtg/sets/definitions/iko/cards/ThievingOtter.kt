package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Thieving Otter
 * {2}{U}
 * Creature — Otter
 * 2/2
 *
 * Whenever this creature deals damage to an opponent, draw a card.
 */
val ThievingOtter = card("Thieving Otter") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Otter"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature deals damage to an opponent, draw a card."

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.Opponent)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Jakub Kasper"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07f84b0a-37d9-4b0f-8d75-1fab45a12d44.jpg?1783931068"
    }
}
