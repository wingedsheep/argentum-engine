package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter

/**
 * Greater Realm of Preservation
 * {1}{W}
 * Enchantment
 * {1}{W}: The next time a black or red source of your choice would deal damage to you this turn,
 *   prevent that damage.
 *
 * Modeling note: the Circle of Protection family shape — a single-instance prevention shield keyed
 * to a source chosen on resolution (`nextInstanceOnly`), constrained to black or red sources.
 */
val GreaterRealmOfPreservation = card("Greater Realm of Preservation") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "{1}{W}: The next time a black or red source of your choice would deal damage to you " +
        "this turn, prevent that damage."

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        effect = Effects.PreventDamage(
            sources = PreventionSourceFilter.Chosen(GameObjectFilter.Any.withAnyColor(Color.BLACK, Color.RED)),
            nextInstanceOnly = true
        )
        description = "{1}{W}: The next time a black or red source of your choice would deal damage to " +
            "you this turn, prevent that damage."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "18"
        artist = "NéNé Thomas"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e236816-0c49-4b48-b18b-03add5a80d72.jpg?1783948084"
    }
}
