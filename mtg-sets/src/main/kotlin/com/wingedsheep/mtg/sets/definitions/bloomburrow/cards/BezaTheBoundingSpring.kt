package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Beza, the Bounding Spring
 * {2}{W}{W}
 * Legendary Creature — Elemental Elk
 * 4/5
 *
 * When Beza enters, create a Treasure token if an opponent controls more lands than you.
 * You gain 4 life if an opponent has more life than you. Create two 1/1 blue Fish creature
 * tokens if an opponent controls more creatures than you. Draw a card if an opponent has
 * more cards in hand than you.
 */
val BezaTheBoundingSpring = card("Beza, the Bounding Spring") {
    manaCost = "{2}{W}{W}"
    typeLine = "Legendary Creature — Elemental Elk"
    power = 4
    toughness = 5
    oracleText = "When Beza enters, create a Treasure token if an opponent controls more lands than you. You gain 4 life if an opponent has more life than you. Create two 1/1 blue Fish creature tokens if an opponent controls more creatures than you. Draw a card if an opponent has more cards in hand than you."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ConditionalEffect(
            condition = Conditions.OpponentControlsMoreLands,
            effect = Effects.CreateTreasure()
        ) then ConditionalEffect(
            condition = Conditions.LessLifeThanOpponent,
            effect = Effects.GainLife(4)
        ) then ConditionalEffect(
            condition = Conditions.OpponentControlsMoreCreatures,
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLUE),
                creatureTypes = setOf("Fish"),
                count = 2,
                imageUri = "https://cards.scryfall.io/normal/front/d/e/de0d6700-49f0-4233-97ba-cef7821c30ed.jpg?1721431109"
            )
        ) then ConditionalEffect(
            condition = Compare(
                DynamicAmount.Count(Player.Opponent, Zone.HAND),
                ComparisonOperator.GT,
                DynamicAmount.Count(Player.You, Zone.HAND)
            ),
            effect = Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "2"
        artist = "Martin Wittfooth"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc310a26-b6a0-4e42-98ab-bdfd7b06cb63.jpg?1721425768"
    }
}
