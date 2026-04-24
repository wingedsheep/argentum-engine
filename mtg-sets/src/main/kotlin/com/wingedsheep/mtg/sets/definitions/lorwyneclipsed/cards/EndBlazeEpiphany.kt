package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * End-Blaze Epiphany
 * {X}{R}
 * Instant
 * End-Blaze Epiphany deals X damage to target creature. When that creature dies this turn,
 * exile a number of cards from the top of your library equal to its power, then choose a
 * card exiled this way. Until the end of your next turn, you may play that card.
 */
val EndBlazeEpiphany = card("End-Blaze Epiphany") {
    manaCost = "{X}{R}"
    typeLine = "Instant"
    oracleText = "End-Blaze Epiphany deals X damage to target creature. When that creature dies this turn, " +
        "exile a number of cards from the top of your library equal to its power, then choose a card exiled this way. " +
        "Until the end of your next turn, you may play that card."

    spell {
        val creature = target("creature", Targets.Creature)
        effect = CompositeEffect(
            listOf(
                Effects.DealDamage(DynamicAmount.XValue, creature),
                CreateDelayedTriggerEffect(
                    trigger = Triggers.Dies,
                    watchedTarget = creature,
                    expiry = DelayedTriggerExpiry.EndOfTurn,
                    effect = CompositeEffect(
                        listOf(
                            GatherCardsEffect(
                                source = CardSource.TopOfLibrary(
                                    count = DynamicAmount.EntityProperty(
                                        entity = EntityReference.Triggering,
                                        numericProperty = EntityNumericProperty.Power
                                    )
                                ),
                                storeAs = "exiled"
                            ),
                            MoveCollectionEffect(
                                from = "exiled",
                                destination = CardDestination.ToZone(Zone.EXILE)
                            ),
                            SelectFromCollectionEffect(
                                from = "exiled",
                                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                                storeSelected = "chosen",
                                prompt = "Choose a card you may play"
                            ),
                            GrantMayPlayFromExileEffect(
                                from = "chosen",
                                untilEndOfNextTurn = true
                            )
                        )
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "134"
        artist = "Tyler Walpole"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f0a90ae-b3b3-4f52-8997-eac514b29e57.jpg?1767952140"
        ruling(
            "2025-11-17",
            "You pay all costs and follow all timing rules for cards played this way. For example, if the chosen " +
                "exiled card is a land card, you may play it only during your main phase while the stack is empty."
        )
    }
}
