package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Oil-Gorger Troll
 * {3}{G}{G}
 * Creature — Phyrexian Troll Warrior
 * 3/4
 *
 * When this creature enters, you gain 3 life. Then if you control a permanent with an oil counter
 * on it, draw a card.
 */
val OilGorgerTroll = card("Oil-Gorger Troll") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Phyrexian Troll Warrior"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, you gain 3 life. Then if you control a permanent with an " +
        "oil counter on it, draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(3) then Effects.If(
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withCounter(CounterType.OIL)),
            then = Effects.DrawCards(1)
        )
        description = "When this creature enters, you gain 3 life. Then if you control a permanent " +
            "with an oil counter on it, draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "177"
        artist = "Dave Kendall"
        flavorText = "Metal or flesh, it didn't matter—seasoned with ichor, it all tasted so good."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78493579-1fad-4664-96d7-195bf59ceef2.jpg?1783918012"
    }
}
