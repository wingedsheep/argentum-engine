package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.core.Zone

/**
 * Haphazard Bombardment
 * {5}{R}
 * Enchantment
 *
 * When Haphazard Bombardment enters the battlefield, choose four nonenchantment permanents
 * you don't control and put an aim counter on each of them.
 * At the beginning of your end step, if two or more permanents you don't control have an aim
 * counter on them, destroy one of those permanents at random.
 *
 * The choice doesn't target — hexproof doesn't prevent aim counters.
 */
val HaphazardBombardment = card("Haphazard Bombardment") {
    manaCost = "{5}{R}"
    typeLine = "Enchantment"
    oracleText = "When Haphazard Bombardment enters the battlefield, choose four nonenchantment permanents you don't control and put an aim counter on each of them.\nAt the beginning of your end step, if two or more permanents you don't control have an aim counter on them, destroy one of those permanents at random."

    // ETB: Choose four nonenchantment permanents you don't control, put aim counters on them
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CompositeEffect(listOf(
            GatherCardsEffect(
                source = CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Nonenchantment.opponentControls()
                ),
                storeAs = "candidates"
            ),
            SelectFromCollectionEffect(
                from = "candidates",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(4)),
                storeSelected = "chosen",
                prompt = "Choose four nonenchantment permanents you don't control",
                useTargetingUI = true
            ),
            Effects.AddCountersToCollection("chosen", Counters.AIM)
        ))
    }

    // End step: If 2+ opponent permanents have aim counters, destroy one at random
    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Compare(
            left = DynamicAmount.AggregateBattlefield(
                player = Player.Opponent,
                filter = GameObjectFilter.Any.withCounter(Counters.AIM),
                aggregation = Aggregation.COUNT
            ),
            operator = ComparisonOperator.GTE,
            right = DynamicAmount.Fixed(2)
        )
        effect = CompositeEffect(listOf(
            GatherCardsEffect(
                source = CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Any.withCounter(Counters.AIM).opponentControls()
                ),
                storeAs = "aim_permanents"
            ),
            SelectFromCollectionEffect(
                from = "aim_permanents",
                selection = SelectionMode.Random(DynamicAmount.Fixed(1)),
                storeSelected = "to_destroy"
            ),
            MoveCollectionEffect(
                from = "to_destroy",
                destination = CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Destroy
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e558a5bd-e8f9-477f-a514-1bf0708f4e9e.jpg?1562744520"
        ruling("2018-04-27", "The nonenchantment permanents that receive aim counters aren't targeted. Permanents with hexproof can be given an aim counter this way.")
        ruling("2018-04-27", "If one or more of the permanents with aim counters on them have indestructible, select the permanent destroyed at random from among the permanents with aim counters that don't have indestructible.")
    }
}
