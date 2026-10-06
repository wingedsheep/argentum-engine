package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter

/**
 * Circle of Protection: Blue
 * {1}{W}
 * Enchantment
 * {1}: The next time a blue source of your choice would deal damage to you this turn, prevent that
 *   damage.
 *
 * Modeling note: the Circle of Protection family (see Circle of Protection: Artifacts) — a
 * single-instance prevention shield keyed to a source chosen on resolution (`nextInstanceOnly`),
 * constrained to blue sources by the `Chosen` eligibility filter.
 */
val CircleOfProtectionBlue = card("Circle of Protection: Blue") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "{1}: The next time a blue source of your choice would deal damage to you this turn, " +
        "prevent that damage."

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.PreventDamage(
            sources = PreventionSourceFilter.Chosen(GameObjectFilter.Any.withColor(Color.BLUE)),
            nextInstanceOnly = true
        )
        description = "{1}: The next time a blue source of your choice would deal damage to you this turn, prevent that damage."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Dameon Willich"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/848b1a7f-e8ba-40b5-92b7-af1e963a0319.jpg?1783948716"
    }
}
