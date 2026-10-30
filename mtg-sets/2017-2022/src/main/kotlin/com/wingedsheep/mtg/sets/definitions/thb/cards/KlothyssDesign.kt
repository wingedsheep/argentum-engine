package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Klothys's Design
 * {5}{G}
 * Sorcery
 * Creatures you control get +X/+X until end of turn, where X is your devotion to green.
 *
 * X is stored once as the spell resolves, then applied to the creatures you control at that moment
 * (both rulings): later devotion changes and creatures that arrive later are unaffected.
 */
val KlothyssDesign = card("Klothys's Design") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Creatures you control get +X/+X until end of turn, where X is your devotion to green. " +
        "(Each {G} in the mana costs of permanents you control counts toward your devotion to green.)"

    spell {
        effect = Effects.Pipeline {
            val x = storeNumber(DynamicAmounts.devotionTo(Color.GREEN))
            run(
                Patterns.Group.modifyStatsForAll(
                    power = x.amount,
                    toughness = x.amount,
                    filter = GroupFilter.AllCreaturesYouControl
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "176"
        artist = "Lie Setiawan"
        flavorText = "The god of destiny grants her followers vision deeper than sight."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1b6b7a7-0ac8-470a-949b-4779ded95359.jpg?1783931537"
        ruling(
            "2020-01-24",
            "Klothys's Design affects only creatures you control at the time it resolves. Creatures you " +
                "begin to control later in the turn won't get +X/+X."
        )
        ruling(
            "2020-01-24",
            "The value of X is determined only as Klothys's Design resolves. Once that happens, the value of " +
                "X won't change later in the turn even if your devotion to green changes."
        )
    }
}
