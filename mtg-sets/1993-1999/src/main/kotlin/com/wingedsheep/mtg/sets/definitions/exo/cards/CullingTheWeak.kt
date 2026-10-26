package com.wingedsheep.mtg.sets.definitions.exo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Culling the Weak
 * {B}
 * Instant
 * As an additional cost to cast this spell, sacrifice a creature.
 * Add {B}{B}{B}{B}.
 */
val CullingTheWeak = card("Culling the Weak") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\nAdd {B}{B}{B}{B}."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        effect = Effects.AddMana(Color.BLACK, 4)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Scott M. Fischer"
        flavorText = "\"The blood of the weak shall be the ink with which we scribe great Yawgmoth's " +
            "glorious name.\"\n—Stronghold architect, journal"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50c33f18-0a5c-4e46-ab0d-6e450915594f.jpg?1783946521"
        ruling("2013-04-15", "You must sacrifice exactly one creature to cast this spell; you cannot cast it without sacrificing a creature, and you cannot sacrifice additional creatures.")
        ruling("2013-04-15", "Players can only respond once this spell has been cast and all its costs have been paid. No one can try to destroy the creature you sacrificed to prevent you from casting this spell.")
    }
}
