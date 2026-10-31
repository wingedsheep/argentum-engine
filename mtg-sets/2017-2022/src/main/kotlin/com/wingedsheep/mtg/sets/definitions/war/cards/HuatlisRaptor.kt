package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Huatli's Raptor
 * {G}{W}
 * Creature — Dinosaur
 * 2/3
 * Vigilance
 * When this creature enters, proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)
 */
val HuatlisRaptor = card("Huatli's Raptor") {
    manaCost = "{G}{W}"
    colorIdentity = "WG"
    typeLine = "Creature — Dinosaur"
    power = 2
    toughness = 3
    oracleText = "Vigilance\n" +
        "When this creature enters, proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Randy Vargas"
        flavorText = "The essence of Ixalan was never far from Huatli's heart."
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba680ad0-6134-46e0-8fcb-532b2052eca7.jpg?1783933395"
    }
}
