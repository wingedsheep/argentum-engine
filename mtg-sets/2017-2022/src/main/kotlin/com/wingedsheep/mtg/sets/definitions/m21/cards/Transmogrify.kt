package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Transmogrify — Core Set 2021 #167 (canonical printing)
 * {3}{R} · Sorcery
 *
 * Exile target creature. That creature's controller reveals cards from the top of their library
 * until they reveal a creature card. That player puts that card onto the battlefield, then
 * shuffles the rest into their library.
 *
 * Polymorph's reveal-until pipeline with an exile in place of the destroy. "That creature's
 * controller" is [Player.ControllerOf] the exiled creature, read from last-known information once
 * it has left the battlefield, so a stolen creature's replacement comes from (and goes to) whoever
 * controlled it. A library with no creature card is fully revealed and simply shuffled (ruling).
 */
val Transmogrify = card("Transmogrify") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Exile target creature. That creature's controller reveals cards from the top of their library until they reveal a creature card. " +
        "That player puts that card onto the battlefield, then shuffles the rest into their library."

    spell {
        val t = target(TargetFilter.Creature)
        val controllerOfTarget = Player.ControllerOf("target creature")
        effect = Effects.Pipeline {
            run(Effects.Exile(t))
            val (found, allRevealed) = gatherUntilMatch(GameObjectFilter.Creature, player = controllerOfTarget)
            // fromZone/toZone mark this as a zone-transition reveal so the client shows the full
            // reveal overlay, including the creature that lands on the battlefield.
            reveal(allRevealed, fromZone = Zone.LIBRARY, toZone = Zone.BATTLEFIELD)
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD, player = controllerOfTarget))
            run(Effects.ShuffleLibrary(EffectTarget.TargetController))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "167"
        artist = "Craig J Spearing"
        flavorText = "\"I'd like to introduce you to the new and improved me.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2fa5133-c238-47e8-befd-3729a3e4303a.jpg?1783930682"
        ruling("2020-06-23", "If the target creature is an illegal target by the time Transmogrify tries to resolve, the spell won't resolve. No player reveals cards or shuffles their library.")
        ruling("2020-06-23", "If the player reveals their entire library without revealing a creature card, that player just shuffles their library.")
    }
}
