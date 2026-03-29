package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Stargaze
 * {X}{B}{B}
 * Sorcery
 * Look at twice X cards from the top of your library. Put X cards from among them
 * into your hand and the rest into your graveyard. You lose X life.
 */
val Stargaze = card("Stargaze") {
    manaCost = "{X}{B}{B}"
    typeLine = "Sorcery"
    oracleText = "Look at twice X cards from the top of your library. Put X cards from among them into your hand and the rest into your graveyard. You lose X life."

    spell {
        effect = CompositeEffect(listOf(
            // Gather twice X cards from top of library
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Multiply(DynamicAmount.XValue, 2)),
                storeAs = "looked"
            ),
            // Select exactly X to keep
            SelectFromCollectionEffect(
                from = "looked",
                selection = SelectionMode.ChooseExactly(DynamicAmount.XValue),
                storeSelected = "kept",
                storeRemainder = "rest",
                selectedLabel = "Put in hand",
                remainderLabel = "Put in graveyard"
            ),
            // Move selected to hand
            MoveCollectionEffect(
                from = "kept",
                destination = CardDestination.ToZone(Zone.HAND)
            ),
            // Move rest to graveyard
            MoveCollectionEffect(
                from = "rest",
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            ),
            // Lose X life
            Effects.LoseLife(DynamicAmount.XValue, EffectTarget.Controller)
        ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Serena Malyon"
        flavorText = "\"Some batfolk dedicate their lives to seeing the world beyond them.\"\n—Warion, scholar of the Cosmos"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/777fc599-8de7-44d2-8fdd-9bddf5948a0c.jpg?1721426524"
    }
}
