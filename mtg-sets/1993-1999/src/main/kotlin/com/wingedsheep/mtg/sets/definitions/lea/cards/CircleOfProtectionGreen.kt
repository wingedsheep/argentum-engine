package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter

/**
 * Circle of Protection: Green
 * {1}{W}
 * Enchantment
 * {1}: The next time a green source of your choice would deal damage to you this turn, prevent
 *   that damage.
 *
 * Modeling note: the Circle of Protection family — a single-instance prevention shield keyed to a
 * source chosen on resolution (`nextInstanceOnly`, not targeted), constrained to green sources by
 * the `Chosen` eligibility filter.
 */
val CircleOfProtectionGreen = card("Circle of Protection: Green") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "{1}: The next time a green source of your choice would deal damage to you this " +
        "turn, prevent that damage."

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.PreventDamage(
            sources = PreventionSourceFilter.Chosen(GameObjectFilter.Any.withColor(Color.GREEN)),
            nextInstanceOnly = true
        )
        description = "{1}: The next time a green source of your choice would deal damage to you this turn, prevent that damage."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Sandra Everingham"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1ae32d20-b438-4f43-b603-e8f706ecfb03.jpg?1783948716"
    }
}
