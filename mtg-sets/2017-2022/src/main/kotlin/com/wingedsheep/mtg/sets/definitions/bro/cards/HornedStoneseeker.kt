package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Horned Stoneseeker
 * {1}{R}
 * Creature — Lizard
 * 2/2
 * Menace
 * When this creature enters, create a tapped Powerstone token.
 * When this creature leaves the battlefield, sacrifice a Powerstone.
 *
 * "Sacrifice a Powerstone" is the bare imperative — the ability's controller sacrifices any
 * Powerstone they control (not necessarily the one this creature made); with none, nothing happens.
 */
val HornedStoneseeker = card("Horned Stoneseeker") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Lizard"
    power = 2
    toughness = 2
    oracleText = "Menace\n" +
        "When this creature enters, create a tapped Powerstone token. " +
        "(It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")\n" +
        "When this creature leaves the battlefield, sacrifice a Powerstone."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreatePowerstone(tapped = true)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.SacrificeOwn(GameObjectFilter.Artifact.withSubtype("Powerstone"))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Eric Velhagen"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cd965a9-caa2-42a3-b2f9-e4f57341ac27.jpg?1783920069"
    }
}
