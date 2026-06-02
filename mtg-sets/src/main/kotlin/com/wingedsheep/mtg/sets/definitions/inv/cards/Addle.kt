package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Addle
 * {1}{B}
 * Sorcery
 * Choose a color. Target player reveals their hand and you choose a card of that
 * color from it. That player discards that card.
 */
val Addle = card("Addle") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose a color. Target player reveals their hand and you choose a card of " +
        "that color from it. That player discards that card."

    spell {
        val targetPlayer = target("target player", TargetPlayer())
        effect = Effects.ChooseColorThen(
            then = Effects.Composite(
                listOf(
                    RevealHandEffect(targetPlayer),
                    GatherCardsEffect(
                        source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                        storeAs = "targetHand",
                    ),
                    SelectFromCollectionEffect(
                        from = "targetHand",
                        selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                        chooser = Chooser.Controller,
                        filter = GameObjectFilter(
                            cardPredicates = listOf(CardPredicate.HasChosenColor),
                        ),
                        storeSelected = "toDiscard",
                        prompt = "Choose a card of the chosen color to discard",
                        alwaysPrompt = true,
                        showAllCards = true,
                    ),
                    MoveCollectionEffect(
                        from = "toDiscard",
                        destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                        moveType = MoveType.Discard,
                    ),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8afb9d0-affa-4599-bf29-729cfe64703b.jpg?1562941705"
    }
}
