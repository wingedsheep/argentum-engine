package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sage of Shaila's Claim — Kaladesh #168
 * {1}{G} · Creature — Elf Druid · 2/1
 *
 * When this creature enters, you get {E}{E}{E} (three energy counters).
 */
val SageOfShailasClaim = card("Sage of Shaila's Claim") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, you get {E}{E}{E} (three energy counters)."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "168"
        artist = "Howard Lyon"
        flavorText = "Even now, Shaila's descendants proudly occupy the greenway that she refused to relinquish to the Consulate."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/0987d29a-2043-4341-89d5-5e34bb6507e2.jpg?1783937173"
    }
}
