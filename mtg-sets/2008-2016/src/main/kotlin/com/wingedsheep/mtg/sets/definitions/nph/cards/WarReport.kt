package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * War Report
 * {3}{W}
 * Instant
 * You gain life equal to the number of creatures on the battlefield plus the number of artifacts
 * on the battlefield.
 *
 * Two independent battlefield counts summed, so an artifact creature is counted twice (ruling).
 */
val WarReport = card("War Report") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "You gain life equal to the number of creatures on the battlefield plus the number " +
        "of artifacts on the battlefield."

    spell {
        effect = Effects.GainLife(
            DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature).count() +
                DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Artifact).count()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "Mike Bierek"
        flavorText = "Underling Ethu's 263rd report read simply \"Yes, my lord. Overwhelmingly, my lord.\" This marked the end of the Mirran-Phyrexian War."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d837262-cd5d-4fc9-96dd-39ed04166883.jpg?1783941322"
        ruling("2011-06-01", "Each artifact creature on the battlefield is counted twice.")
    }
}
