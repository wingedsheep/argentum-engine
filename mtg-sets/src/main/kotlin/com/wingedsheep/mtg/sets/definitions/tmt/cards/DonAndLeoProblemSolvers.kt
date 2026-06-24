package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Don & Leo, Problem Solvers
 * {3}{W/U}{W/U}
 * Legendary Creature — Mutant Ninja Turtle
 * 4/6
 *
 * Vigilance
 * At the beginning of your end step, exile up to one target artifact you control
 * and up to one target creature you control. Then return them to the battlefield
 * under their owners' control.
 */
val DonAndLeoProblemSolvers = card("Don & Leo, Problem Solvers") {
    manaCost = "{3}{W/U}{W/U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Mutant Ninja Turtle"
    oracleText = "Vigilance\nAt the beginning of your end step, exile up to one target artifact you control and up to one target creature you control. Then return them to the battlefield under their owners' control."
    power = 4
    toughness = 6

    keywords(Keyword.VIGILANCE)

    // Exile both targets first, then return both — so the two re-enter simultaneously and
    // see each other's ETBs. Declined ("up to one") targets simply no-op.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        val artifact = target(
            "up to one target artifact you control",
            TargetObject(count = 1, optional = true, filter = TargetFilter(GameObjectFilter.Artifact.youControl()))
        )
        val creature = target(
            "up to one target creature you control",
            TargetCreature(optional = true, filter = TargetFilter.CreatureYouControl)
        )
        effect = Effects.Move(artifact, Zone.EXILE)
            .then(Effects.Move(creature, Zone.EXILE))
            .then(Effects.Move(artifact, Zone.BATTLEFIELD))
            .then(Effects.Move(creature, Zone.BATTLEFIELD))
        description = "At the beginning of your end step, exile up to one target artifact you control and up to one target creature you control. Then return them to the battlefield under their owners' control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "143"
        artist = "Néstor Ossandón Leal"
        flavorText = "Technician. Tactician."
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d239beaf-4f5b-494e-be5c-ffa8b6e28bde.jpg?1769006269"
    }
}
