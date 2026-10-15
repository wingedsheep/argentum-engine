package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Queza, Augur of Agonies
 * {1}{W}{U}{B}
 * Legendary Creature — Octopus Advisor
 * 3 / 4
 * Whenever you draw a card, target opponent loses 1 life and you gain 1 life.
 */
val QuezaAugurOfAgonies = card("Queza, Augur of Agonies") {
    manaCost = "{1}{W}{U}{B}"
    colorIdentity = "BUW"
    typeLine = "Legendary Creature — Octopus Advisor"
    oracleText = "Whenever you draw a card, target opponent loses 1 life and you gain 1 life."
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.you.draws()
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(1, opponent) then Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        artist = "Josh Hass"
        flavorText = "Some try to unravel the tangled skeins of fate. She reweaves them to her own deadly design."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7edb7a26-82be-4efd-9a5c-a3816e1ee2d6.jpg"
    }
}
