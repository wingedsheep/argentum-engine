package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Blackmail
 * {B}
 * Sorcery
 * Target player reveals three cards from their hand and you choose one of them.
 * That player discards that card.
 */
val Blackmail = card("Blackmail") {
    manaCost = "{B}"
    typeLine = "Sorcery"
    oracleText = "Target player reveals three cards from their hand and you choose one of them. That player discards that card."

    spell {
        target = TargetPlayer()
        effect = CompositeEffect(
            listOf(
                // 1. Gather all cards from target player's hand
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "hand"
                ),
                // 2. Target player chooses 3 to reveal (auto-selects all if ≤3, skips if empty)
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(3)),
                    chooser = Chooser.TargetPlayer,
                    storeSelected = "revealed"
                ),
                // 3. Controller chooses 1 to discard
                SelectFromCollectionEffect(
                    from = "revealed",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    storeSelected = "toDiscard"
                ),
                // 4. Move chosen card to target player's graveyard
                MoveCollectionEffect(
                    from = "toDiscard",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                    moveType = MoveType.Discard
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Christopher Moeller"
        flavorText = "\"Even the most virtuous person is only one secret away from being owned by the Cabal.\""
        imageUri = "https://cards.scryfall.io/large/front/9/b/9b40f6eb-e2a4-46d2-8822-b0f3dc508b73.jpg?1562931568"
    }
}
