package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Transmutation — Legends #123
 * {1}{B} · Instant
 *
 * Switch target creature's power and toughness until end of turn.
 */
val Transmutation = card("Transmutation") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Switch target creature's power and toughness until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.SwitchPowerToughness(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Susan Van Camp"
        flavorText = "\"You know what I was,/ You see what I am: change me, change me!\" —Randall"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/329f7eb2-eadf-46ec-aed4-63152051f3c1.jpg?1783948061"
        ruling("2021-03-19", "Effects that switch a creature's power and toughness apply after all other effects, regardless of when those effects began to apply. For instance, if you target a 1/2 creature then give it +2/+0 later in the turn, it's a 2/3 creature, not a 4/1 creature.")
        ruling("2021-03-19", "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to a creature may become lethal if you switch its power and toughness during that turn.")
        ruling("2021-03-19", "Switching a creature's power and toughness twice (or any even number of times) effectively returns the creature to the power and toughness it had before any switches.")
        ruling("2005-11-01", "Note that the wording \"Effects that alter the creature's power alter its toughness instead, and vice versa, this turn\" has been removed.")
    }
}
