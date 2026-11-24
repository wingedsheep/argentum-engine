package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Coursers' Accord
 * {4}{G}{W}
 * Sorcery
 *
 * Create a 3/3 green Centaur creature token, then populate. (Create a token that's a copy of a
 * creature token you control.)
 *
 * The Centaur is [Effects.CreateToken] (as on Call of the Conclave); populate chooses — never
 * targets — a creature token you control on resolution, so the Centaur just created is always a
 * legal choice: [Effects.CreateTokenCopyOfChosenPermanent] over creature tokens, as on Scion of
 * Vitu-Ghazi.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 */
val CoursersAccord = card("Coursers' Accord") {
    manaCost = "{4}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Sorcery"
    oracleText = "Create a 3/3 green Centaur creature token, then populate. (Create a token that's a " +
        "copy of a creature token you control.)"

    spell {
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Centaur"),
        ) then Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Creature.token())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Nils Hamm"
        flavorText = "Bleak rumors reached Trostani's ears. She decreed that no one should travel alone."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f027ceb0-5d2b-4cf6-87ad-e6b9b1e20634.jpg?1783940342"
    }
}
