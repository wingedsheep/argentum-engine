package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Mishra's Onslaught
 * {3}{R}
 * Instant
 * Choose one —
 * • Create two 1/1 colorless Soldier artifact creature tokens.
 * • Creatures you control get +2/+0 until end of turn.
 */
val MishrasOnslaught = card("Mishra's Onslaught") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Create two 1/1 colorless Soldier artifact creature tokens.\n" +
        "• Creatures you control get +2/+0 until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("Create two 1/1 colorless Soldier artifact creature tokens") {
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    creatureTypes = setOf("Soldier"),
                    count = 2,
                    artifactToken = true
                )
            }
            mode("Creatures you control get +2/+0 until end of turn") {
                effect = Patterns.Group.modifyStatsForAll(2, 0, GroupFilter.AllCreaturesYouControl)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "143"
        artist = "Josu Hernaiz"
        flavorText = "\"Hah. Urza has underestimated my strength yet again.\"\n—Mishra"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/eda82e38-c1d4-4019-8f79-f91602d941b0.jpg"
    }
}
