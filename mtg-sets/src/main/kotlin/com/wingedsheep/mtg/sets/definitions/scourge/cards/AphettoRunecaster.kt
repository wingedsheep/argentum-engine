package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.GameEvent.TurnFaceUpEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec

/**
 * Aphetto Runecaster
 * {3}{U}
 * Creature — Human Wizard
 * 2/3
 * Whenever a permanent is turned face up, you may draw a card.
 */
val AphettoRunecaster = card("Aphetto Runecaster") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 3
    oracleText = "Whenever a permanent is turned face up, you may draw a card."

    triggeredAbility {
        trigger = TriggerSpec(TurnFaceUpEvent, TriggerBinding.ANY)
        effect = MayEffect(DrawCardsEffect(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Eric Peterson"
        flavorText = "Unraveling the mysteries of a morph is its own reward."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c5de028-91ce-48d8-8557-ae12542adea2.jpg?1562531185"
    }
}
