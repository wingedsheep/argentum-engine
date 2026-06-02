package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Whiskervale Forerunner
 * {3}{W}
 * Creature — Mouse Bard
 * 3/4
 *
 * Valiant — Whenever this creature becomes the target of a spell or ability you
 * control for the first time each turn, look at the top five cards of your library.
 * You may reveal a creature card with mana value 3 or less from among them. You may
 * put it onto the battlefield if it's your turn. If you don't put it onto the
 * battlefield, put it into your hand. Put the rest on the bottom of your library in
 * a random order.
 */
val WhiskervaleForerunner = card("Whiskervale Forerunner") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Mouse Bard"
    power = 3
    toughness = 4
    oracleText = "Valiant — Whenever this creature becomes the target of a spell or ability you control for the first time each turn, look at the top five cards of your library. You may reveal a creature card with mana value 3 or less from among them. You may put it onto the battlefield if it's your turn. If you don't put it onto the battlefield, put it into your hand. Put the rest on the bottom of your library in a random order."

    // Valiant trigger: look at top 5, may pick a creature MV≤3
    // If your turn: choose battlefield or hand. If not your turn: hand.
    triggeredAbility {
        trigger = Triggers.Valiant
        effect = Effects.Composite(listOf(
            // Look at top 5
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(5)),
                storeAs = "looked"
            ),
            // May reveal a creature with MV ≤ 3
            SelectFromCollectionEffect(
                from = "looked",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                filter = GameObjectFilter.Creature.manaValueAtMost(3),
                storeSelected = "kept",
                storeRemainder = "rest",
                selectedLabel = "Reveal",
                remainderLabel = "Put on bottom"
            ),
            // Rest on bottom in random order
            MoveCollectionEffect(
                from = "rest",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom)
            ),
            // If your turn: choose to put on battlefield or hand
            // If not your turn: put in hand
            ConditionalEffect(
                condition = IsYourTurn,
                effect = Effects.Composite(listOf(
                    SelectFromCollectionEffect(
                        from = "kept",
                        selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                        storeSelected = "toBattlefield",
                        storeRemainder = "toHand",
                        selectedLabel = "Put onto the battlefield",
                        remainderLabel = "Put into your hand"
                    ),
                    MoveCollectionEffect(
                        from = "toBattlefield",
                        destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                        revealed = true
                    ),
                    MoveCollectionEffect(
                        from = "toHand",
                        destination = CardDestination.ToZone(Zone.HAND),
                        revealed = true
                    )
                )),
                elseEffect = MoveCollectionEffect(
                    from = "kept",
                    destination = CardDestination.ToZone(Zone.HAND),
                    revealed = true
                )
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60a78d59-af31-4af9-95aa-2573fe553925.jpg?1721426007"
    }
}
