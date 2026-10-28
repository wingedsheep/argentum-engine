package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Collective Drone
 * {1}{B}
 * Artifact Creature — Borg
 * Power: *  Toughness: 2
 *
 * Collective Drone's power is equal to the number of artifact creatures you control.
 */
val CollectiveDrone = card("Collective Drone") {
    manaCost = "{1}{B}"
    typeLine = "Artifact Creature — Borg"
    colorIdentity = "B"
    oracleText = "Collective Drone's power is equal to the number of artifact creatures you control."
    toughness = 2
    dynamicPower(DynamicAmounts.battlefield(Player.You, GameObjectFilter.ArtifactCreature).count())
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "José Parodi"
        flavorText = "\"You will be assimilated.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65b74daa-4606-4118-9752-b0dcd7a3f486.jpg?1785981469"
    }
}
