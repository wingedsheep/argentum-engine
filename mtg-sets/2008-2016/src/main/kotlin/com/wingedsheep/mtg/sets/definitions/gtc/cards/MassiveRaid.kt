package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Massive Raid
 * {1}{R}{R}
 * Instant
 * Massive Raid deals damage to any target equal to the number of creatures you control.
 *
 * The count is taken on resolution (2013-01-24 ruling).
 */
val MassiveRaid = card("Massive Raid") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Massive Raid deals damage to any target equal to the number of creatures you control."

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.creaturesYouControl(), t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Zoltan Boros"
        flavorText = "\"The Boros lack vision. Give them a convenient scapegoat and they'll be blind to the true threats.\"\n—Lazav"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b16fbd8-fb62-4f75-92b3-a6295d95b327.jpg?1783940123"
        ruling(
            "2013-01-24",
            "Count the number of creatures you control when Massive Raid resolves to determine how much " +
                "damage is dealt."
        )
    }
}
