package com.wingedsheep.mtg.sets.definitions.nec.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Aerial Surveyor — Neon Dynasty Commander #5 (canonical printing)
 * {2}{W} · Artifact — Vehicle · 3/4
 *
 * Flying
 * Whenever this Vehicle attacks, if defending player controls more lands than you, search your
 * library for a basic Plains card, put it onto the battlefield tapped, then shuffle.
 * Crew 2
 *
 * The Loyal Warhound land-ramp search behind an intervening "if" that compares the defending
 * player's land count to yours — checked both when the attack trigger would fire and again on
 * resolution.
 */
val AerialSurveyor = card("Aerial Surveyor") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Vehicle"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever this Vehicle attacks, if defending player controls more lands than you, search " +
        "your library for a basic Plains card, put it onto the battlefield tapped, then shuffle.\n" +
        "Crew 2"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.DefendingPlayer, GameObjectFilter.Land).count(),
            ComparisonOperator.GT,
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land).count(),
        )
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.PLAINS),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "5"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c474f31-336d-44df-a7da-27aefd220e22.jpg?1783923999"
        ruling(
            "2022-02-18",
            "Aerial Surveyor's triggered ability has an intervening \"if\" clause. That means (1) the " +
                "ability won't trigger at all unless the defending player controls more lands than you, " +
                "and (2) the ability will do nothing if you control at least as many lands as the " +
                "defending player by the time it resolves."
        )
    }
}
