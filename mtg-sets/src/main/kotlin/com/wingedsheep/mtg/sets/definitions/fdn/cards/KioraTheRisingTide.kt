package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Kiora, the Rising Tide
 * {2}{U}
 * Legendary Creature — Merfolk Noble
 * 3/2
 *
 * When Kiora enters, draw two cards, then discard two cards.
 * Threshold — Whenever Kiora attacks, if there are seven or more cards in your graveyard,
 * you may create Scion of the Deep, a legendary 8/8 blue Octopus creature token.
 */
val KioraTheRisingTide = card("Kiora, the Rising Tide") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Merfolk Noble"
    power = 3
    toughness = 2
    oracleText = "When Kiora enters, draw two cards, then discard two cards.\nThreshold — Whenever Kiora attacks, if there are seven or more cards in your graveyard, you may create Scion of the Deep, a legendary 8/8 blue Octopus creature token."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = HandPatterns.loot(draw = 2, discard = 2)
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.CardsInGraveyardAtLeast(7)
        optional = true
        effect = Effects.CreateToken(
            power = 8,
            toughness = 8,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Octopus"),
            legendary = true,
            imageUri = "https://cards.scryfall.io/normal/front/2/6/26c478f7-b426-4055-8d06-e5782226c826.jpg?1740922203"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "Julian Kok Joon Wen"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83f20a32-9f5d-4a68-8995-549e57554da2.jpg?1730488754"
    }
}
