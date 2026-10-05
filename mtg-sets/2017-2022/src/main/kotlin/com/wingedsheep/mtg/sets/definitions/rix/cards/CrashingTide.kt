package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Crashing Tide
 * {2}{U}
 * Sorcery
 * This spell has flash as long as you control a Merfolk.
 * Return target creature to its owner's hand.
 * Draw a card.
 *
 * The flash clause is `conditionalFlash` — a timing permission read while the card is in hand. The
 * bare "Merfolk" names a *permanent* with that subtype, not specifically a creature. If the target
 * is illegal on resolution the spell doesn't resolve and you don't draw.
 */
val CrashingTide = card("Crashing Tide") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "This spell has flash as long as you control a Merfolk.\n" +
        "Return target creature to its owner's hand.\n" +
        "Draw a card."

    conditionalFlash = Conditions.YouControl(
        GameObjectFilter.Permanent.withSubtype(Subtype.MERFOLK)
    )

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ReturnToHand(creature) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Carmen Sinek"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e312980a-7b3c-43f8-81cf-53ff818cea30.jpg?1783935328"
        ruling(
            "2018-01-19",
            "Once you announce that you're casting Crashing Tide, players can't try to remove your Merfolk to make it lose flash until you're done casting it. If it loses flash after it's been cast, it will still resolve if able.",
        )
        ruling(
            "2018-01-19",
            "If the target creature is an illegal target by the time Crashing Tide tries to resolve, the spell doesn't resolve. You won't draw a card.",
        )
    }
}
