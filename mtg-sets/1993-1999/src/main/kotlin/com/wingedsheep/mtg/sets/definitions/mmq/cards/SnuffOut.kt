package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Snuff Out
 * {3}{B}
 * Instant
 * If you control a Swamp, you may pay 4 life rather than pay this spell's mana cost.
 * Destroy target nonblack creature. It can't be regenerated.
 *
 * The pitch cost is a [SelfAlternativeCost] of {0} plus 4 life, offered only while you control a
 * permanent with the Swamp land subtype (Force of Negation's conditional shape). Its mana value
 * stays 4 whichever cost is paid.
 */
val SnuffOut = card("Snuff Out") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "If you control a Swamp, you may pay 4 life rather than pay this spell's mana cost.\n" +
        "Destroy target nonblack creature. It can't be regenerated."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(Costs.additional.PayLife(4)),
        condition = Conditions.YouControl(GameObjectFilter.Land.withSubtype("Swamp")),
    )

    spell {
        val t = target(TargetFilter.Creature.notColor(Color.BLACK))
        effect = Effects.Destroy(t, noRegenerate = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "162"
        artist = "Mike Ploog"
        flavorText = "Squee watched his Kyren cousins fall with a mixture of sympathy and relief."
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18a3cca1-e50e-49b6-9e1a-f86640e3b177.jpg?1783945946"
    }
}
