package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Sulu, Space Swashbuckler
 * {1}{W}
 * Legendary Creature — Human Pilot
 * 2/2
 *
 * First strike
 * Whenever Sulu becomes tapped, if an Equipment or Spacecraft entered the battlefield under your
 * control this turn, draw a card.
 *
 * The intervening "if" reads the per-player entry log, so an Equipment or Spacecraft that entered
 * and has since left still counts. "Equipment or Spacecraft" is one any-of amount, so a permanent
 * with both subtypes counts once (irrelevant to a >= 1 check, but the faithful shape).
 */
val SuluSpaceSwashbuckler = card("Sulu, Space Swashbuckler") {
    manaCost = "{1}{W}"
    typeLine = "Legendary Creature — Human Pilot"
    colorIdentity = "W"
    oracleText = "First strike\nWhenever Sulu becomes tapped, if an Equipment or Spacecraft entered the battlefield under your control this turn, draw a card."
    power = 2
    toughness = 2

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.subtypesEnteredUnderControlThisTurn(
                setOf(Subtype.EQUIPMENT, Subtype("Spacecraft"))
            ),
            ComparisonOperator.GTE,
            1,
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Micah Epstein"
        flavorText = "\"Oh my.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b16f967-4bc5-4508-8433-b46323ce2e71.jpg?1793101366"
    }
}
