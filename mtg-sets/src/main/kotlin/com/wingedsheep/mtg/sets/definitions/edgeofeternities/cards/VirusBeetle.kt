package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Virus Beetle
 * {1}{B}
 * Artifact Creature — Insect
 * When this creature enters, each opponent discards a card.
 * 1/1
 */
val VirusBeetle = card("Virus Beetle") {
    manaCost = "{1}{B}"
    typeLine = "Artifact Creature — Insect"
    oracleText = "When this creature enters, each opponent discards a card."
    power = 1
    toughness = 1

    // ETB ability: each opponent discards a card
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.EachOpponentDiscards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Leesha Hannigan"
        flavorText = "The Eumidian biotechs were delighted when a mutated malware gene devastated their systems. They'd encourage the trait, so long as future clutches could be contained."
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e96c986c-684c-4546-a9c9-b6b903bda101.jpg?1752947055"
    }
}
