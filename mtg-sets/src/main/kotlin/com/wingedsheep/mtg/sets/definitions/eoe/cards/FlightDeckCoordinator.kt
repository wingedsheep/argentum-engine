package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Flight-Deck Coordinator
 * {2}{W}
 * Creature — Human Soldier
 * 3/3
 *
 * At the beginning of your end step, if you control two or more tapped creatures, you gain 2 life.
 */
val FlightDeckCoordinator = card("Flight-Deck Coordinator") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "At the beginning of your end step, if you control two or more tapped creatures, you gain 2 life."

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature.tapped()),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(2)
        )
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Diego Gisbert"
        flavorText = "The hidden might of the Sunstar Free Company is its technical staff."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88cc6328-a035-4f74-b786-390e5c7c324c.jpg?1752946621"

        ruling(
            "2025-07-25",
            "Flight-Deck Coordinator's ability will check as your end step starts to see if you " +
                "control two or more tapped creatures. If you don't, the ability won't trigger at " +
                "all. You won't be able to tap anything during your end step in time to have the " +
                "ability trigger. If you don't control two or more tapped creatures when the " +
                "ability resolves, the ability won't do anything."
        )
    }
}
