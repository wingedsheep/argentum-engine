package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.AnyCondition
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Sothera, the Supervoid
 * {2}{B}{B}
 * Legendary Enchantment
 *
 * Whenever a creature you control dies, each opponent chooses a creature they control and exiles it.
 * At the beginning of your end step, if a player controls no creatures, sacrifice Sothera, then put
 * a creature card exiled with it onto the battlefield under your control with two additional
 * +1/+1 counters on it.
 *
 * Ability 2: Linked exile is gathered BEFORE the sacrifice so entity IDs are captured before
 * Rule 400.7 strips LinkedExileComponent from Sothera when it moves to the graveyard.
 */
val SotheraTheSupervoid = card("Sothera, the Supervoid") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Enchantment"
    oracleText = "Whenever a creature you control dies, each opponent chooses a creature they control and exiles it.\n" +
        "At the beginning of your end step, if a player controls no creatures, sacrifice Sothera, then put a creature card exiled with it onto the battlefield under your control with two additional +1/+1 counters on it."

    triggeredAbility {
        trigger = Triggers.YourCreatureDies
        effect = ForEachPlayerEffect(
            players = Player.EachOpponent,
            effects = listOf(
                GatherCardsEffect(
                    source = CardSource.BattlefieldMatching(
                        filter = GameObjectFilter.Creature,
                        player = Player.You
                    ),
                    storeAs = "opponent_creatures"
                ),
                SelectFromCollectionEffect(
                    from = "opponent_creatures",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "chosen_creature",
                    prompt = "Choose a creature to exile"
                ),
                MoveCollectionEffect(
                    from = "chosen_creature",
                    destination = CardDestination.ToZone(Zone.EXILE),
                    linkToSource = true
                )
            )
        )
        description = "Whenever a creature you control dies, each opponent chooses a creature they control and exiles it."
    }

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = AnyCondition(
            listOf(
                Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature, negate = true),
                Exists(Player.Opponent, Zone.BATTLEFIELD, GameObjectFilter.Creature, negate = true)
            )
        )
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromLinkedExile(),
                    storeAs = "exiled_creatures"
                ),
                SacrificeSelfEffect,
                SelectFromCollectionEffect(
                    from = "exiled_creatures",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    filter = GameObjectFilter.Creature,
                    storeSelected = "chosen_creature",
                    prompt = "Choose a creature card to put onto the battlefield"
                ),
                MoveCollectionEffect(
                    from = "chosen_creature",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                    storeMovedAs = "returned_creature"
                ),
                AddCountersToCollectionEffect(
                    collectionName = "returned_creature",
                    counterType = Counters.PLUS_ONE_PLUS_ONE,
                    count = 2
                )
            )
        )
        description = "At the beginning of your end step, if a player controls no creatures, sacrifice Sothera, then put a creature card exiled with it onto the battlefield under your control with two additional +1/+1 counters on it."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "115"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e99d6fc0-dcf2-4b25-81c2-02c230a36246.jpg?1752947018"
    }
}
