package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Scourge of Fleets — Journey into Nyx #51 (canonical printing)
 * {5}{U}{U}
 * Creature — Kraken
 * 6/6
 * When this creature enters, return each creature your opponents control with toughness X or less
 * to its owner's hand, where X is the number of Islands you control.
 *
 * A non-targeted mass bounce ([Patterns.Group.returnAllToHand]) over opposing creatures whose
 * projected toughness is at most the Island count. Both X (the Island land subtype, nonbasic
 * Islands included) and each creature's toughness are read when the trigger resolves, per the
 * ruling; every matching creature returns simultaneously, each to its owner's hand.
 */
val ScourgeOfFleets = card("Scourge of Fleets") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Kraken"
    power = 6
    toughness = 6
    oracleText = "When this creature enters, return each creature your opponents control with toughness X " +
        "or less to its owner's hand, where X is the number of Islands you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Group.returnAllToHand(
            GroupFilter(
                GameObjectFilter.Creature.opponentControls().compareNumericProperty(
                    CardNumericProperty.TOUGHNESS,
                    ComparisonOperator.LTE,
                    DynamicAmounts.landsWithSubtype(Subtype.ISLAND),
                )
            )
        )
        description = "When this creature enters, return each creature your opponents control with toughness X " +
            "or less to its owner's hand, where X is the number of Islands you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0aaf1de7-1476-4f8d-b8b3-67675891df23.jpg?1783939442"
        ruling("2014-04-26", "The value of X is determined as the ability resolves.")
    }
}
