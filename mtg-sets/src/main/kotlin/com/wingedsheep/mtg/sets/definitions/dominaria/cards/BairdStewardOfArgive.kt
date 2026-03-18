package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AttackTax

/**
 * Baird, Steward of Argive
 * {2}{W}{W}
 * Legendary Creature — Human Soldier
 * 2/4
 * Vigilance
 * Creatures can't attack you or planeswalkers you control unless their controller
 * pays {1} for each of those creatures.
 */
val BairdStewardOfArgive = card("Baird, Steward of Argive") {
    manaCost = "{2}{W}{W}"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 4
    oracleText = "Vigilance\nCreatures can't attack you or planeswalkers you control unless their controller pays {1} for each of those creatures."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = AttackTax(manaCostPerAttacker = "{1}")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Christine Choi"
        flavorText = "\"The walls of Argive were built by a peaceful dynastic union that has guarded us against war for fifteen centuries. The lesson is clear.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1a9594e-edc9-42b2-ba8a-8298da9441fb.jpg?1562743396"
    }
}
