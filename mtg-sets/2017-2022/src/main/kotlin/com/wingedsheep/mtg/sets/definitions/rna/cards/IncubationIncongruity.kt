package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Incubation // Incongruity (RNA 226) — split-layout spell (CR 709).
 *
 * Incubation {G/U} — Sorcery
 *   Look at the top five cards of your library. You may reveal a creature card from among them
 *   and put it into your hand. Put the rest on the bottom of your library in a random order.
 *
 * Incongruity {1}{G}{U} — Instant
 *   Exile target creature. That creature's controller creates a 3/3 green Frog Lizard creature token.
 *
 * Incubation is [Patterns.Library.lookAtTopRevealMatchingToHand] (random-order bottom by default).
 * Incongruity's token goes to [EffectTarget.TargetController], which reads the exiled creature's
 * last-known controller (the Rapid Hybridization / Unyielding Gatekeeper shape).
 */
val IncubationIncongruity = card("Incubation // Incongruity") {
    layout = CardLayout.SPLIT
    colorIdentity = "GU"

    face("Incubation") {
        manaCost = "{G/U}"
        typeLine = "Sorcery"
        oracleText = "Look at the top five cards of your library. You may reveal a creature card from among them " +
            "and put it into your hand. Put the rest on the bottom of your library in a random order."

        spell {
            effect = Patterns.Library.lookAtTopRevealMatchingToHand(
                count = 5,
                filter = GameObjectFilter.Creature,
                prompt = "You may reveal a creature card from among them and put it into your hand"
            )
        }
    }

    face("Incongruity") {
        manaCost = "{1}{G}{U}"
        typeLine = "Instant"
        oracleText = "Exile target creature. That creature's controller creates a 3/3 green Frog Lizard creature token."

        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Exile(creature) then
                Effects.CreateToken(
                    power = 3,
                    toughness = 3,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Frog", "Lizard"),
                    controller = EffectTarget.TargetController,
                    imageUri = "https://cards.scryfall.io/normal/front/4/9/495d3391-6311-418e-b55a-10c0af751026.jpg?1783933604"
                )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "226"
        artist = "Mike Bierek"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd3a4d6e-34ae-4047-a9c7-11e28b0a276d.jpg?1783933630"
        ruling(
            "2019-01-25",
            "If the target creature is an illegal target by the time Incongruity tries to resolve, " +
                "the spell doesn't resolve. No player creates a Frog Lizard token."
        )
    }
}
