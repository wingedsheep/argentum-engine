package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility

/**
 * Forth Eorlingas!
 * {X}{R}{W}
 * Sorcery
 *
 * Create X 2/2 red Human Knight creature tokens with trample and haste.
 * Whenever one or more creatures you control deal combat damage to one or more players this turn,
 * you become the monarch.
 *
 * The "this turn" trigger is an end-of-turn global ability on the offensive combat-damage batch:
 * it fires once per combat-damage step however many creatures connect and players are hit, and
 * it watches every creature you control, not just the tokens.
 */
val ForthEorlingas = card("Forth Eorlingas!") {
    manaCost = "{X}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Sorcery"
    oracleText = "Create X 2/2 red Human Knight creature tokens with trample and haste.\n" +
        "Whenever one or more creatures you control deal combat damage to one or more players " +
        "this turn, you become the monarch."

    spell {
        effect = Effects.CreateToken(
            count = DynamicAmounts.xValue(),
            power = 2,
            toughness = 2,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Human", "Knight"),
            keywords = setOf(Keyword.TRAMPLE, Keyword.HASTE),
            imageUri = "https://cards.scryfall.io/normal/front/4/9/491fc1c3-a46e-4cfd-a749-57f4c96f6aea.jpg?1783916057"
        ) then Effects.CreateGlobalTriggeredAbility(
            duration = Duration.EndOfTurn,
            ability = TriggeredAbility.create(
                trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToOneOrMorePlayers(),
                effect = Effects.BecomeMonarch()
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Filipe Pagliuso"
        flavorText = "\"A sword-day, a red day, ere the sun rises! Ride now, ride now! Ride to Gondor!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06c053d3-028e-4961-93a5-5b7bb5a8601c.jpg?1783916020"
    }
}
