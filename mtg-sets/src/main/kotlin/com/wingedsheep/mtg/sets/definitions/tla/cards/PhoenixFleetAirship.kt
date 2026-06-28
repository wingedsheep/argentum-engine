package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Phoenix Fleet Airship
 * {2}{B}{B}
 * Artifact — Vehicle
 * 4/4
 *
 * Flying
 * At the beginning of your end step, if you sacrificed a permanent this turn, create a token
 * that's a copy of this Vehicle.
 * As long as you control eight or more permanents named Phoenix Fleet Airship, this Vehicle is
 * an artifact creature.
 * Crew 1
 */
val PhoenixFleetAirship = card("Phoenix Fleet Airship") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "At the beginning of your end step, if you sacrificed a permanent this turn, create a " +
        "token that's a copy of this Vehicle.\n" +
        "As long as you control eight or more permanents named Phoenix Fleet Airship, this " +
        "Vehicle is an artifact creature.\n" +
        "Crew 1"

    keywords(Keyword.FLYING)

    // Intervening-if on the per-player "permanents sacrificed this turn" counter.
    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.YouSacrificedPermanentsThisTurn()
        effect = Effects.CreateTokenCopyOfSelf()
    }

    // Conditional type change: becomes an artifact creature while you control 8+ copies.
    staticAbility {
        condition = Conditions.YouControlAtLeast(8, GameObjectFilter.Any.named("Phoenix Fleet Airship"))
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "114"
        artist = "Thanh Tuấn"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b51d3259-c41c-4f64-9666-0a9e676c812f.jpg?1764120785"
    }
}
