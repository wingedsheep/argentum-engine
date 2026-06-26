package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Commune with Beavers
 * {G}
 * Sorcery
 *
 * Look at the top three cards of your library. You may reveal an artifact, creature, or land
 * card from among them and put it into your hand. Put the rest on the bottom of your library
 * in any order.
 */
val CommuneWithBeavers = card("Commune with Beavers") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top three cards of your library. You may reveal an artifact, creature, or land card from among them and put it into your hand. Put the rest on the bottom of your library in any order."

    spell {
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = DynamicAmount.Fixed(3),
            filter = GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Land,
            prompt = "You may reveal an artifact, creature, or land card and put it into your hand",
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "182"
        artist = "hippo"
        flavorText = "\"Guy speak beaver.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/8/784287c2-43c5-4210-93ae-cdd33b9acb1b.jpg?1748706441"
    }
}
