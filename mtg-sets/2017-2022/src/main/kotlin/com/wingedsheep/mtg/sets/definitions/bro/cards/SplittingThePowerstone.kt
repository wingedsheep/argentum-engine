package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Splitting the Powerstone
 * {2}{U}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice an artifact.
 * Create two tapped Powerstone tokens. If the sacrificed artifact was legendary, draw a card.
 *
 * `Conditions.SacrificedWasLegendary` reads the supertypes frozen onto the cost-time snapshot of the
 * sacrificed artifact (the Nasty End mechanism), so the answer is the artifact as it last existed.
 */
val SplittingThePowerstone = card("Splitting the Powerstone") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice an artifact.\n" +
        "Create two tapped Powerstone tokens. If the sacrificed artifact was legendary, draw a card. " +
        "(The tokens are artifacts with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Artifact))

    spell {
        effect = Effects.CreatePowerstone(2, tapped = true) then
            Effects.If(
                condition = Conditions.SacrificedWasLegendary,
                then = Effects.DrawCards(1)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Campbell White"
        flavorText = "As the glowing stone broke apart, so too did the last shred of the brothers' bond."
        imageUri = "https://cards.scryfall.io/normal/front/5/6/569dab90-a0d8-4c0a-8e32-b2cb3aec39a6.jpg?1783920104"
    }
}
