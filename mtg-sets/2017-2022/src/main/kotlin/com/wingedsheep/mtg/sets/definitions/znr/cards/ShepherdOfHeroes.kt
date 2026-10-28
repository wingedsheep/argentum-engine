package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity

/**
 * Shepherd of Heroes — Zendikar Rising #38
 * {4}{W} · Creature — Angel Cleric · 3/4
 *
 * Flying
 * When this creature enters, you gain 2 life for each creature in your party.
 */
val ShepherdOfHeroes = card("Shepherd of Heroes") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Cleric"
    oracleText = "Flying\nWhen this creature enters, you gain 2 life for each creature in your party. " +
        "(Your party consists of up to one each of Cleric, Rogue, Warrior, and Wizard.)"
    power = 3
    toughness = 4

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(DynamicAmounts.partySize() * 2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Livia Prima"
        flavorText = "\"Rest, friends. I will protect you.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f70baa3-5232-4e10-80b4-a9ead6ed6ec3.jpg?1783929406"
    }
}
