package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SpellCostReduction

/**
 * Wizard's Lightning
 * {2}{R}
 * Instant
 * This spell costs {2} less to cast if you control a Wizard.
 * Wizard's Lightning deals 3 damage to any target.
 */
val WizardsLightning = card("Wizard's Lightning") {
    manaCost = "{2}{R}"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast if you control a Wizard.\nWizard's Lightning deals 3 damage to any target."

    staticAbility {
        ability = SpellCostReduction(
            CostReductionSource.FixedIfControlFilter(
                amount = 2,
                filter = GameObjectFilter.Any.withSubtype("Wizard")
            )
        )
    }

    spell {
        val t = target("any target", Targets.Any)
        effect = Effects.DealDamage(3, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Grzegorz Rutkowski"
        flavorText = "\"The study of magic began when the first mage taught herself to throw lightning.\" —Naban, dean of iteration"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59bf371a-164c-4db8-9207-197c2e7c3c10.jpg?1562736134"
    }
}
