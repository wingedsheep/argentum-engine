package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter

/**
 * Circle of Protection: White
 * {1}{W}
 * Enchantment
 * {1}: The next time a white source of your choice would deal damage to you this turn, prevent
 *   that damage.
 *
 * Modeling note: the Circle of Protection family — a single-instance prevention shield keyed to a
 * source chosen on resolution (`nextInstanceOnly`, not targeted), constrained to white sources by
 * the `Chosen` eligibility filter.
 */
val CircleOfProtectionWhite = card("Circle of Protection: White") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "{1}: The next time a white source of your choice would deal damage to you this " +
        "turn, prevent that damage."

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.PreventDamage(
            sources = PreventionSourceFilter.Chosen(GameObjectFilter.Any.withColor(Color.WHITE)),
            nextInstanceOnly = true
        )
        description = "{1}: The next time a white source of your choice would deal damage to you this turn, prevent that damage."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92df19c9-e127-42d9-8dd2-7fa5a7095428.jpg?1783948715"
    }
}
