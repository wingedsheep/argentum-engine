package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter

/**
 * Circle of Protection: Red
 * {1}{W}
 * Enchantment
 * {1}: The next time a red source of your choice would deal damage to you this turn, prevent that
 *   damage.
 *
 * Modeling note: the Circle of Protection family (see Circle of Protection: Artifacts) — a
 * single-instance prevention shield keyed to a source chosen on resolution (`nextInstanceOnly`),
 * constrained to red sources by the `Chosen` eligibility filter.
 */
val CircleOfProtectionRed = card("Circle of Protection: Red") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "{1}: The next time a red source of your choice would deal damage to you this turn, " +
        "prevent that damage."

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.PreventDamage(
            sources = PreventionSourceFilter.Chosen(GameObjectFilter.Any.withColor(Color.RED)),
            nextInstanceOnly = true
        )
        description = "{1}: The next time a red source of your choice would deal damage to you this turn, prevent that damage."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3dd94c5-42f6-4148-be6e-2a3a4226cc0e.jpg?1783948715"
    }
}
