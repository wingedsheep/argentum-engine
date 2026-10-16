package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * One with the Machine
 * {3}{U}
 * Sorcery
 *
 * Draw cards equal to the greatest mana value among artifacts you control.
 *
 * MAX mana-value aggregation over artifacts you control, read on resolution; no artifacts → draw 0.
 * A permanent with {X} in its mana cost has mana value 0 on the battlefield (per ruling).
 */
val OneWithTheMachine = card("One with the Machine") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw cards equal to the greatest mana value among artifacts you control."

    spell {
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).maxManaValue()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Chase Stone"
        flavorText = "\"When I grafted the Planar Bridge into myself I felt my Planeswalker spark flare beyond my body. The Multiverse was my plaything. It felt . . . incredible.\"\n—Tezzeret"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17f2aaf5-6c1f-4663-865f-6cdd5640485a.jpg?1783934583"
        ruling("2018-07-13", "If a permanent has {X} in its mana cost, X is considered to be 0.")
    }
}
