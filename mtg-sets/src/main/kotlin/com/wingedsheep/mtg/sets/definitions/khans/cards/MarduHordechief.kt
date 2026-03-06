package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.YouAttackedThisTurn
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Mardu Hordechief
 * {2}{W}
 * Creature — Human Warrior
 * 2/3
 * Raid — When Mardu Hordechief enters the battlefield, if you attacked this turn,
 * create a 1/1 white Warrior creature token.
 */
val MarduHordechief = card("Mardu Hordechief") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 3
    oracleText = "Raid — When Mardu Hordechief enters, if you attacked this turn, create a 1/1 white Warrior creature token."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = YouAttackedThisTurn
        effect = CreateTokenEffect(
            count = 1,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Warrior")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Torstein Nordstrand"
        flavorText = "\"The horde grows with each assault.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23426221-30a8-4be2-9c70-f0eb022edad7.jpg?1562783652"
    }
}
