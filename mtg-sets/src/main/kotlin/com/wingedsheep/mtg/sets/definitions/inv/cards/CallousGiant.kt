package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.AmountFilter
import com.wingedsheep.sdk.scripting.events.RecipientFilter

/**
 * Callous Giant
 * {4}{R}{R}
 * Creature — Giant
 * 4/4
 * If a source would deal 3 or less damage to this creature, prevent that damage.
 *
 * Invasion engine gap #7: the all-or-nothing damage threshold is expressed with the new
 * [AmountFilter] on [EventPattern.DamageEvent] — the prevention only fires when the would-be
 * amount is 3 or less, otherwise the full amount lands.
 */
val CallousGiant = card("Callous Giant") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    power = 4
    toughness = 4
    oracleText = "If a source would deal 3 or less damage to this creature, prevent that damage."

    replacementEffect(
        PreventDamage(
            appliesTo = EventPattern.DamageEvent(
                recipient = RecipientFilter.Self,
                amount = AmountFilter.AtMost(3)
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "139"
        artist = "Mark Brill"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/330028c4-8e91-4fe3-a87d-1660dfd2507e.jpg?1562905295"
    }
}
