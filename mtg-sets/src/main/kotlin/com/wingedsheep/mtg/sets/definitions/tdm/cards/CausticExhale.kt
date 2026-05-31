package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost

/**
 * Caustic Exhale
 * {B}
 * Instant
 *
 * As an additional cost to cast this spell, behold a Dragon or pay {1}.
 * (To behold a Dragon, choose a Dragon you control or reveal a Dragon card from your hand.)
 * Target creature gets -3/-3 until end of turn.
 */
val CausticExhale = card("Caustic Exhale") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, behold a Dragon or pay {1}. " +
        "(To behold a Dragon, choose a Dragon you control or reveal a Dragon card from your hand.)\n" +
        "Target creature gets -3/-3 until end of turn."

    additionalCost(
        AdditionalCost.BeholdOrPay(
            filter = Filters.WithSubtype("Dragon"),
            alternativeManaCost = "{1}"
        )
    )

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.ModifyStats(-3, -3, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Camille Alquier"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/488152ce-2048-4ccb-b2d6-b9628958286f.jpg?1743204258"
    }
}
