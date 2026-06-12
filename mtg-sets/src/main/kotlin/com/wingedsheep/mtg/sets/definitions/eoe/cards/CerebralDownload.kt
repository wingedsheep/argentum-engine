package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Cerebral Download
 * {4}{U}
 * Instant
 * Surveil X, where X is the number of artifacts you control. Then draw three cards.
 */
val CerebralDownload = card("Cerebral Download") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Surveil X, where X is the number of artifacts you control. Then draw three cards."

    spell {
        val artifactCount = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count()
        effect = Effects.Pipeline {
            val surveiled = gather(CardSource.TopOfLibrary(artifactCount), name = "surveiled")
            val (toGraveyard, toTop) = chooseUpToSplit(
                artifactCount, from = surveiled,
                selectedLabel = "Put in graveyard",
                remainderLabel = "Put on top",
                name = "toGraveyard",
                remainderName = "toTop"
            )
            move(toGraveyard, CardDestination.ToZone(Zone.GRAVEYARD))
            move(
                toTop,
                CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top),
                order = CardOrder.ControllerChooses
            )
            run(Effects.DrawCards(3))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Antonio José Manzanedo"
        flavorText = "All Uthros networks are routed through an ancient relic called a memory vessel, which converts data into thought."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d3b5d73-694c-4f9a-8b4f-d8d8c58c8d65.jpg?1752946739"
    }
}
