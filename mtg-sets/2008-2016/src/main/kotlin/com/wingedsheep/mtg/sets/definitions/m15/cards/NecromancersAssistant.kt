package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Necromancer's Assistant
 * {2}{B}
 * Creature — Zombie
 * 3/1
 * When this creature enters, mill three cards.
 */
val NecromancersAssistant = card("Necromancer's Assistant") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 3
    toughness = 1
    oracleText = "When this creature enters, mill three cards."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Svetlin Velinov"
        flavorText = "Zombies and necromancers agree: easy access to brains is preferred."
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b6a8b3e-2709-4036-838b-56cc82b0d917.jpg"
    }
}
