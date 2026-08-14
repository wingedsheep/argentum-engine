package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Kraven, Proud Predator
 * {1}{R}{G}
 * Legendary Creature — Human Warrior Villain
 * Power/toughness: star/4
 *
 * Vigilance
 * Top of the Food Chain — Kraven's power is equal to the greatest mana value among
 * permanents you control.
 *
 * The characteristic-defining power is a Layer-7b CDA reading the fluent battlefield
 * aggregate [DynamicAmounts.battlefield(Player.You).maxManaValue()] — the greatest mana
 * value among permanents you control (Kraven himself is a permanent you control, so his
 * own mana value 3 is included in the max). Only power is dynamic; toughness stays a
 * printed 4, so we use the single-stat `dynamicPower(...)` helper (as Duelist of the Mind
 * does) rather than the both-stats `dynamicStats(...)`.
 */
val KravenProudPredator = card("Kraven, Proud Predator") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Human Warrior Villain"
    oracleText = "Vigilance\n" +
        "Top of the Food Chain — Kraven's power is equal to the greatest mana value " +
        "among permanents you control."
    toughness = 4
    dynamicPower(DynamicAmounts.battlefield(Player.You).maxManaValue())

    keywords(Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Alexander Mokhov"
        flavorText = "The hunter seeks only the greatest prey—only Spider-Man's death will quench Kraven's thirst for glory."
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af7c63e1-eccc-40a2-ba14-ce0d2a6123fc.jpg?1783905316"
    }
}
