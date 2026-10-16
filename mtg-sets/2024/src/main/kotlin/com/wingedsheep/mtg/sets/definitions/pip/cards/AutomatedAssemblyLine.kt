package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Automated Assembly Line — Fallout #10.
 * {1}{W} · Artifact
 *
 * Whenever one or more artifact creatures you control deal combat damage to a player, you get {E}.
 * Pay {E}{E}{E}: Create a tapped 3/3 colorless Robot artifact creature token.
 *
 * The first ability is the once-per-damaged-player combat-damage batch trigger; its "you control"
 * is implied by the observer, so the source filter is plain `ArtifactCreature`.
 */
val AutomatedAssemblyLine = card("Automated Assembly Line") {
    manaCost = "{1}{W}"
    typeLine = "Artifact"
    oracleText = "Whenever one or more artifact creatures you control deal combat damage to a player, " +
        "you get {E} (an energy counter).\n" +
        "Pay {E}{E}{E}: Create a tapped 3/3 colorless Robot artifact creature token."

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.ArtifactCreature).dealCombatDamageToAPlayer()
        effect = Effects.GetEnergy(1)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 3)
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            creatureTypes = setOf("Robot"),
            tapped = true,
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/7/6/76cde21f-2f01-4560-972b-7ecd6740d6de.jpg?1783912042",
        )
        description = "Pay {E}{E}{E}: Create a tapped 3/3 colorless Robot artifact creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "David Álvarez"
        flavorText = "\"You needn't be afraid of me. It's my securitrons that are going to kill you.\"\n—Mr. House"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe9e2a76-1324-4b8e-b23c-10c04aca1870.jpg?1783912419"
    }
}
