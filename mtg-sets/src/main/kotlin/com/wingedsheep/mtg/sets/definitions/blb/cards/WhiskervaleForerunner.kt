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
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.CollectionSlot
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
        effect = Effects.Pipeline {
            // Look at top 5
            val looked = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(5)), name = "looked")
            // May reveal a creature with MV ≤ 3
            val (kept, rest) = chooseUpToSplit(
                1, from = looked,
                filter = GameObjectFilter.Creature.manaValueAtMost(3),
                selectedLabel = "Reveal",
                remainderLabel = "Put on bottom",
                name = "kept",
                remainderName = "rest"
            )
            // Rest on bottom in random order
            move(rest, CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom))
            // If your turn: choose to put on battlefield or hand
            // If not your turn: put in hand
            run(
                ConditionalEffect(
                    condition = IsYourTurn,
                    effect = Effects.Pipeline {
                        val (toBattlefield, toHand) = chooseUpToSplit(
                            1, from = CollectionSlot("kept"),
                            selectedLabel = "Put onto the battlefield",
                            remainderLabel = "Put into your hand",
                            name = "toBattlefield",
                            remainderName = "toHand"
                        )
                        move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD), revealed = true)
                        move(toHand, CardDestination.ToZone(Zone.HAND), revealed = true)
                    },
                    elseEffect = MoveCollectionEffect(
                        from = "kept",
                        destination = CardDestination.ToZone(Zone.HAND),
                        revealed = true
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60a78d59-af31-4af9-95aa-2573fe553925.jpg?1721426007"
    }
}
