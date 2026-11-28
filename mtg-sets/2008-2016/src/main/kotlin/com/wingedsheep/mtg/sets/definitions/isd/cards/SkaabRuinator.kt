package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones

/**
 * Skaab Ruinator
 * {1}{U}{U}
 * Creature — Zombie Horror
 * 5/6
 * As an additional cost to cast this spell, exile three creature cards from your graveyard.
 * Flying
 * You may cast this card from your graveyard.
 */
val SkaabRuinator = card("Skaab Ruinator") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Horror"
    oracleText = "As an additional cost to cast this spell, exile three creature cards from your graveyard.\n" +
        "Flying\n" +
        "You may cast this card from your graveyard."
    power = 5
    toughness = 6

    // The card is on the stack while its costs are paid, so the exile pool never contains it —
    // even when it is cast from the graveyard (the engine excludes the cast card from the pool).
    additionalCost(
        Costs.additional.ExileCards(
            count = 3,
            filter = GameObjectFilter.Creature,
            fromZone = CostZone.GRAVEYARD
        )
    )

    keywords(Keyword.FLYING)

    staticAbility {
        ability = MayCastSelfFromZones(zones = listOf(Zone.GRAVEYARD))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "77"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44c40cdc-a11a-47df-b902-d8fbe9014d03.jpg?1783940965"
        ruling("2011-09-22", "Skaab Ruinator is on the stack when you pay its costs. It can't be exiled to pay for itself.")
        ruling("2011-09-22", "You must exile three creature cards from your graveyard no matter what zone you're casting Skaab Ruinator from.")
    }
}
