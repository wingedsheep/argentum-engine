package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Molten Birth — Magic 2014 #147
 * {1}{R}{R} · Sorcery
 *
 * Create two 1/1 red Elemental creature tokens, then flip a coin. If you win the flip, return
 * Molten Birth to its owner's hand.
 *
 * - The return happens *during* resolution: `CardSource.Entity(EffectTarget.Self)` gathers the
 *   resolving spell off the stack and moves it to its **owner's** hand (`Player.OwnerOfSource`), so
 *   it never reaches the graveyard (2018-12-07 ruling). On a lost flip it goes to the graveyard as
 *   usual.
 */
val MoltenBirth = card("Molten Birth") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Create two 1/1 red Elemental creature tokens, then flip a coin. If you win the flip, " +
        "return Molten Birth to its owner's hand."

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Elemental"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/e/d/ed37fa8e-5930-4e57-8ead-73894ac40c19.jpg?1783939882"
        ) then Effects.FlipCoin(
            wonEffect = Effects.Pipeline {
                toHand(gather(CardSource.Entity(EffectTarget.Self)), Player.OwnerOfSource)
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Jaime Jones"
        flavorText = "\"Few can coax the creatures within the magma to be cooled and take form.\"\n—Koth of the Hammer"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cd182be-1604-47e1-858f-3c304fd0ee63.jpg?1783939912"
        ruling(
            "2018-12-07",
            "If you win the flip, Molten Birth goes directly from the stack to its owner's hand. " +
                "It never goes to a graveyard."
        )
    }
}
