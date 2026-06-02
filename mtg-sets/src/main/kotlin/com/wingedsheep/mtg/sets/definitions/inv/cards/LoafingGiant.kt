package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Loafing Giant
 * {4}{R}
 * Creature — Giant
 * 4/6
 * Whenever this creature attacks or blocks, mill a card. If a land card was milled this
 * way, prevent all combat damage this creature would deal this turn.
 *
 * Composed from the standard mill pipeline (GatherCards → MoveCollection to graveyard)
 * gated by [Conditions.CollectionContainsMatch] on the milled card, mirroring Cache Grab.
 * "Attacks or blocks" is two SELF triggers sharing the same effect.
 */
val LoafingGiant = card("Loafing Giant") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    power = 4
    toughness = 6
    oracleText = "Whenever this creature attacks or blocks, mill a card. If a land card was milled this way, prevent all combat damage this creature would deal this turn."

    val millAndMaybePrevent = Effects.Composite(
        listOf(
            // Mill a card: gather top card, move it to the graveyard.
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                storeAs = "milled"
            ),
            MoveCollectionEffect(
                from = "milled",
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            ),
            // If a land card was milled this way, prevent all combat damage this creature
            // would deal this turn.
            ConditionalEffect(
                condition = Conditions.CollectionContainsMatch("milled", GameObjectFilter.Land),
                effect = Effects.PreventCombatDamageFrom(GroupFilter.source())
            )
        )
    )

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = millAndMaybePrevent
    }

    triggeredAbility {
        trigger = Triggers.Blocks
        effect = millAndMaybePrevent
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Greg Hildebrandt & Tim Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fab5f738-04d0-44c9-88ec-28469b668040.jpg?1562945565"
    }
}
