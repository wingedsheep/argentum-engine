package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Chronicler of Heroes
 * {1}{G}{W}
 * Creature — Centaur Wizard
 * 3/3
 * When this creature enters, draw a card if you control a creature with a +1/+1 counter on it.
 *
 * Not an intervening "if": the condition is checked only as the ability resolves, so it's an
 * [Effects.If] gate inside the ETB trigger over [Conditions.YouControl] with a `withCounter`
 * creature filter.
 */
val ChroniclerOfHeroes = card("Chronicler of Heroes") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Centaur Wizard"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, draw a card if you control a creature with a +1/+1 counter on it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(
            condition = Conditions.YouControl(
                GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE)
            ),
            then = Effects.DrawCards(1)
        )
        description = "When this creature enters, draw a card if you control a creature with a +1/+1 counter on it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "John Stanko"
        flavorText = "She paints pictures with words, though not all pictures show the truth."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de1cbee6-61f4-40f0-86fe-93831a0b87d8.jpg?1783939731"
        ruling("2017-11-17", "Whether you control a creature with a +1/+1 counter on it is checked only when Chronicler of Heroes's ability resolves.")
    }
}
