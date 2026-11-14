package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cleric of the Forward Order — Magic Origins #10 (canonical printing)
 * {1}{W} · Creature — Human Cleric · 2/2 · Common
 *
 * When this creature enters, you gain 2 life for each creature you control named
 * Cleric of the Forward Order.
 *
 * Festival of Trokin's doubled battlefield count, narrowed by `named(...)`. The count is taken on
 * resolution and includes the Cleric itself while it is still on the battlefield.
 */
val ClericOfTheForwardOrder = card("Cleric of the Forward Order") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you gain 2 life for each creature you control named Cleric of the Forward Order."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.named("Cleric of the Forward Order")
            ).count() * 2
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Chris Rallis"
        flavorText = "\"War after war, a healer's enemy is always the same—death itself.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cfcea783-e9f0-4d44-ac74-5aee9a07aa69.jpg?1783938363"
        ruling(
            "2022-12-08",
            "Count the number of creatures named Cleric of the Forward Order you control as the ability resolves to determine how much life you gain. If the Cleric of the Forward Order with the ability that triggered is still on the battlefield, it will count itself."
        )
    }
}
