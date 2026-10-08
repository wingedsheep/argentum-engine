package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gaea's Courser
 * {4}{G}
 * Creature — Centaur Soldier
 * 4/5
 * Whenever this creature attacks, if there are three or more creature cards in your graveyard, draw a card.
 *
 * An intervening-if (CR 603.4): checked when the attack trigger would fire and again on resolution.
 */
val GaeasCourser = card("Gaea's Courser") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Centaur Soldier"
    power = 4
    toughness = 5
    oracleText = "Whenever this creature attacks, if there are three or more creature cards in your graveyard, draw a card."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.CreatureCardsInGraveyardAtLeast(3)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Michele Giorgi"
        flavorText = "\"Even the leaves oppose your being here. What hope have you against nature's might?\""
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a0b94e5-d270-4820-83b9-1d346d378ff8.jpg"
    }
}
