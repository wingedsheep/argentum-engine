package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Fecund Greenshell
 * {3}{G}{G}
 * Creature — Elemental Turtle
 * 4/6
 *
 * Reach
 *
 * As long as you control ten or more lands, creatures you control get +2/+2.
 *
 * Whenever this creature or another creature you control with toughness greater
 * than its power enters, look at the top card of your library. If it's a land
 * card, you may put it onto the battlefield tapped. Otherwise, put it into your hand.
 *
 * Note: The ETB trigger is simplified to fire for any creature you control entering
 * (including self). The "toughness greater than power" filter is not yet supported
 * as a CardPredicate, so this is a minor approximation.
 */
val FecundGreenshell = card("Fecund Greenshell") {
    manaCost = "{3}{G}{G}"
    typeLine = "Creature — Elemental Turtle"
    power = 4
    toughness = 6
    oracleText = "Reach\nAs long as you control ten or more lands, creatures you control get +2/+2.\nWhenever this creature or another creature you control with toughness greater than its power enters, look at the top card of your library. If it's a land card, you may put it onto the battlefield tapped. Otherwise, put it into your hand."

    keywords(Keyword.REACH)

    // Conditional lord: 10+ lands → creatures you control get +2/+2
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStatsForCreatureGroup(
                powerBonus = 2,
                toughnessBonus = 2,
                filter = GroupFilter.AllCreaturesYouControl
            ),
            condition = Conditions.ControlLandsAtLeast(10)
        )
    }

    // ETB: look at top card, if land may put onto battlefield tapped, else hand
    // Simplified trigger: fires for any creature you control entering (toughness > power
    // restriction not yet supported as a predicate)
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )

        effect = CompositeEffect(listOf(
            // Look at top 1 card
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                storeAs = "looked"
            ),
            // Split into land and non-land
            FilterCollectionEffect(
                from = "looked",
                filter = CollectionFilter.MatchesFilter(GameObjectFilter.Land),
                storeMatching = "landCards",
                storeNonMatching = "nonLandCards"
            ),
            // For lands: player may put onto battlefield tapped, or keep in hand
            SelectFromCollectionEffect(
                from = "landCards",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "toBattlefield",
                storeRemainder = "landsToHand",
                selectedLabel = "Put onto the battlefield tapped",
                remainderLabel = "Put into your hand"
            ),
            MoveCollectionEffect(
                from = "toBattlefield",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped)
            ),
            MoveCollectionEffect(
                from = "landsToHand",
                destination = CardDestination.ToZone(Zone.HAND)
            ),
            // Non-lands go to hand
            MoveCollectionEffect(
                from = "nonLandCards",
                destination = CardDestination.ToZone(Zone.HAND)
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Kisung Koh"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80b3e815-0e2e-4325-b1d5-5531b7b92da6.jpg?1721426804"

        ruling("2024-07-26", "Damage remains marked on creatures until the turn ends. If Fecund Greenshell's second ability stops applying, then any creatures that needed the toughness bonus to stay alive will die.")
        ruling("2024-07-26", "Fecund Greenshell's last ability checks the power and toughness of a creature only at the moment it enters.")
        ruling("2024-07-26", "If the top card of your library is a land card, you may choose to put it into your hand rather than onto the battlefield tapped.")
    }
}
