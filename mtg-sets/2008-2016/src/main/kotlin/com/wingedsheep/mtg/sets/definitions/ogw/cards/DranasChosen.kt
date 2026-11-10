package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Drana's Chosen
 * {3}{B}
 * Creature — Vampire Shaman Ally
 * 2/2
 * Cohort — {T}, Tap an untapped Ally you control: Create a tapped 2/2 black Zombie creature token.
 */
val DranasChosen = card("Drana's Chosen") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Shaman Ally"
    power = 2
    toughness = 2
    oracleText = "Cohort — {T}, Tap an untapped Ally you control: Create a tapped 2/2 black Zombie creature token."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapPermanents(1, GameObjectFilter.Permanent.withSubtype(Subtype.ALLY)),
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            tapped = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Deruchenko Alexander"
        flavorText = "\"Drana commands me. I command my nulls. And so her bidding is done.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/8/189abe2f-5899-438b-9f54-3c98e50f037a.jpg"
    }
}
