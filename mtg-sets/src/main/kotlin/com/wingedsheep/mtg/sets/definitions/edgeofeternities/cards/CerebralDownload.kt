package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SelectionMode
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
    typeLine = "Instant"
    oracleText = "Surveil X, where X is the number of artifacts you control. Then draw three cards."

    spell {
        val artifactCount = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count()
        val surveilEffect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(artifactCount),
                    storeAs = "surveiled"
                ),
                SelectFromCollectionEffect(
                    from = "surveiled",
                    selection = SelectionMode.ChooseUpTo(artifactCount),
                    storeSelected = "toGraveyard",
                    storeRemainder = "toTop",
                    selectedLabel = "Put in graveyard",
                    remainderLabel = "Put on top"
                ),
                MoveCollectionEffect(
                    from = "toGraveyard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD)
                ),
                MoveCollectionEffect(
                    from = "toTop",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top),
                    order = CardOrder.ControllerChooses
                )
            )
        )
        effect = surveilEffect then Effects.DrawCards(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Antonio José Manzanedo"
        flavorText = "All Uthros networks are routed through an ancient relic called a memory vessel, which converts data into thought."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d3b5d73-694c-4f9a-8b4f-d8d8c58c8d65.jpg?1752946739"
    }
}
