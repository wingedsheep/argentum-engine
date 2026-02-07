package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalEffect

/**
 * Thoughtbound Primoc
 * {2}{U}
 * Creature — Bird Beast
 * 2/3
 * Flying
 * At the beginning of your upkeep, if you control a Wizard,
 * draw a card, then discard a card.
 */
val ThoughtboundPrimoc = card("Thoughtbound Primoc") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Bird Beast"
    power = 2
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = ConditionalEffect(
            condition = Conditions.ControlCreatureOfType(Subtype("Wizard")),
            effect = Effects.Loot()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Mark Tedin"
        flavorText = "It has learned to anticipate its master's wishes."
        imageUri = "https://cards.scryfall.io/large/front/e/8/e89156b5-8bdb-41d1-a7aa-63f770a9b070.jpg?1562950377"
    }
}
