package com.wingedsheep.mtg.sets.definitions.ody.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.model.Rarity

/**
 * Traumatize
 * {3}{U}{U}
 * Sorcery
 * Target player mills half their library, rounded down.
 *
 * The half is read off the targeted player's library at resolution (Fleet Swallower's shape, with
 * the rounded-down [div] instead of `divRoundedUp`).
 */
val Traumatize = card("Traumatize") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target player mills half their library, rounded down."

    spell {
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(
            DynamicAmounts.zone(player.asPlayer, Zone.LIBRARY).count() / 2,
            player
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "110"
        artist = "Greg Staples"
        flavorText = "Dreads had half a mind to leave."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4cad8762-ff91-4971-84be-f643c7795679.jpg?1783945252"
    }
}
