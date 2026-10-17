package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Cut Your Losses
 * {4}{U}{U}
 * Sorcery
 * Casualty 2 (As you cast this spell, you may sacrifice a creature with power 2 or greater. When you do, copy this spell and you may choose a new target for the copy.)
 * Target player mills half their library, rounded down.
 *
 * Casualty 2 (CR 702.153) is the printed [KeywordAbility.Casualty]. The mill count is read at
 * resolution off the targeted player's library (same shape as Jidoor's Overture), so a copy aimed
 * at the same player mills half, then the original mills half of what remains.
 */
val CutYourLosses = card("Cut Your Losses") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Casualty 2 (As you cast this spell, you may sacrifice a creature with power 2 or greater. When you do, copy this spell and you may choose a new target for the copy.)\nTarget player mills half their library, rounded down."

    keywordAbility(KeywordAbility.casualty(2))

    spell {
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(
            count = DynamicAmounts.zone(player.asPlayer, Zone.LIBRARY).count() / 2,
            target = player
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31184dd0-4080-44c1-a3e0-f6d10a4d1fcb.jpg?1783923147"

        ruling(
            "2022-04-29",
            "If you cast Cut Your Losses and copy it targeting the same player, that player will mill " +
                "half their library, then mill half of what remains after that. They do not mill their " +
                "entire library."
        )
        ruling(
            "2022-04-29",
            "If you pay the casualty cost of a spell, the copy will resolve before the original spell."
        )
    }
}
