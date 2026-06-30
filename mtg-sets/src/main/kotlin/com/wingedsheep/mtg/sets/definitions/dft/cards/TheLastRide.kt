package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The Last Ride
 * {B}
 * Legendary Artifact — Vehicle
 * 13/13
 * The Last Ride gets -X/-X, where X is your life total.
 * {2}{B}, Pay 2 life: Draw a card.
 * Crew 2
 */
val TheLastRide = card("The Last Ride") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Legendary Artifact — Vehicle"
    power = 13
    toughness = 13
    oracleText = "The Last Ride gets -X/-X, where X is your life total.\n" +
        "{2}{B}, Pay 2 life: Draw a card.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"

    staticAbility {
        // -X/-X where X is your life total, applied to this Vehicle itself.
        val negLife = DynamicAmount.Multiply(DynamicAmount.YourLifeTotal, -1)
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = negLife,
            toughnessBonus = negLife
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.PayLife(2))
        effect = Effects.DrawCards(1)
        description = "{2}{B}, Pay 2 life: Draw a card."
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "94"
        artist = "Michele Giorgi"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cbb7b4e-bd32-44a0-9396-16738c5e4381.jpg"
    }
}
