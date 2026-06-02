package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.references.Player.OwnerOf
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Chaos Warp
 * {2}{R}
 * Instant
 *
 * The owner of target permanent shuffles it into their library, then reveals the
 * top card of their library. If it's a permanent card, they put it onto the
 * battlefield.
 */
val ChaosWarp = card("Chaos Warp") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "The owner of target permanent shuffles it into their library, then reveals the top card of their library. If it's a permanent card, they put it onto the battlefield."

    spell {
        val permanent = target("permanent", Targets.Permanent)

        effect = Effects.Composite(
            listOf(
                Effects.ShuffleIntoLibrary(permanent),
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(
                        count = DynamicAmount.Fixed(1),
                        player = OwnerOf("target permanent")
                    ),
                    storeAs = "revealed",
                    revealed = true
                ),
                SelectFromCollectionEffect(
                    from = "revealed",
                    selection = SelectionMode.All,
                    filter = GameObjectFilter.Permanent,
                    storeSelected = "permanentCard",
                    storeRemainder = null
                ),
                MoveCollectionEffect(
                    from = "permanentCard",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                    underOwnersControl = true
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a050532-1e75-4212-acb0-ec740378af10.jpg?1721428740"
    }
}
