package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Pah-Wraith
 * {5}{R}{R}
 * Creature — Wraith
 * 4/4
 *
 * First strike, haste
 * Strange new worlds — Whenever this creature attacks, creatures you control get +X/+0 until end
 * of turn, where X is the number of differently named lands you control.
 *
 * "Strange new worlds" is an ability word with no rules meaning. X is counted as the trigger
 * resolves, and the bonus applies to the creatures you control at that time.
 */
val PahWraith = card("Pah-Wraith") {
    manaCost = "{5}{R}{R}"
    typeLine = "Creature — Wraith"
    power = 4
    toughness = 4
    oracleText = "First strike, haste\n" +
        "Strange new worlds — Whenever this creature attacks, creatures you control get +X/+0 until " +
        "end of turn, where X is the number of differently named lands you control."

    keywords(Keyword.FIRST_STRIKE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Group.modifyStatsForAll(
            DynamicAmounts.differentlyNamedLandsYouControl(),
            DynamicAmounts.fixed(0),
            Filters.Group.creaturesYouControl
        )
        description = "Strange new worlds — creatures you control get +X/+0 until end of turn, " +
            "where X is the number of differently named lands you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Nino Vecia"
        flavorText = "Know power. Know adoration. All you must do is accept their love."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24c7e179-c016-4db8-ad8b-aa5629152b5c.jpg?1793013595"
    }
}
