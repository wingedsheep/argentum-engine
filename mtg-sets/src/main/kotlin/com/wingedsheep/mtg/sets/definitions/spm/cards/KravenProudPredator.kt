package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Kraven, Proud Predator
 * {1}{R}{G}
 * Legendary Creature — Human Warrior Villain
 * Vigilance
 * Kraven, Proud Predator's power is equal to the greatest mana value among permanents you control.
 * [printed power: *, toughness: 4]
 */
val KravenProudPredator = card("Kraven, Proud Predator") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Warrior Villain"
    oracleText = "Vigilance\nKraven, Proud Predator's power is equal to the greatest mana value among permanents you control."

    dynamicPower = CharacteristicValue.dynamic(
        DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent).maxManaValue()
    )
    toughness = 4

    keywords(Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Marvel"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84f5c1db-00f4-4572-b7c5-63d39b8ec406.jpg?1748706434"
    }
}
