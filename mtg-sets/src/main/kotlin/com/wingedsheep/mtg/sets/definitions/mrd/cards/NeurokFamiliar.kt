package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Neurok Familiar — Mirrodin #43
 * {1}{U} · Creature — Bird · 1/1
 *
 * Flying
 * When this creature enters, reveal the top card of your library. If it's an artifact card, put it
 * into your hand. Otherwise, put it into your graveyard.
 *
 * Gather → Select → Move, the same shape as Skirk Drill Sergeant: the top card is gathered *revealed*
 * so both players see it either way, then partitioned by [GameObjectFilter.Artifact]. Whichever side
 * of the partition is empty simply moves nothing, so an empty library resolves as a no-op rather than
 * failing.
 */
val NeurokFamiliar = card("Neurok Familiar") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "When this creature enters, reveal the top card of your library. If it's an artifact card, " +
        "put it into your hand. Otherwise, put it into your graveyard."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                    storeAs = "revealed",
                    revealed = true
                ),
                SelectFromCollectionEffect(
                    from = "revealed",
                    selection = SelectionMode.All,
                    filter = GameObjectFilter.Artifact,
                    storeSelected = "artifact",
                    storeRemainder = "nonArtifact"
                ),
                MoveCollectionEffect(
                    from = "artifact",
                    destination = CardDestination.ToZone(Zone.HAND)
                ),
                MoveCollectionEffect(
                    from = "nonArtifact",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Edward P. Beard, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52fd15b0-4ada-41c7-81e0-4f8956798685.jpg?1783944553"
    }
}
