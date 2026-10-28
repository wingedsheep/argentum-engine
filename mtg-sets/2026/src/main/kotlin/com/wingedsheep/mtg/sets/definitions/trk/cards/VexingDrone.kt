package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Vexing Drone
 * {1}{W}
 * Artifact Creature — Borg
 * 2/2
 *
 * When this creature enters, tap target creature an opponent controls.
 */
val VexingDrone = card("Vexing Drone") {
    manaCost = "{1}{W}"
    typeLine = "Artifact Creature — Borg"
    colorIdentity = "W"
    oracleText = "When this creature enters, tap target creature an opponent controls."
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature.opponentControls())
        effect = Effects.Tap(creature)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Kristina WermoongRey"
        flavorText = "\"Freedom is irrelevant. Self-determination is irrelevant. You must comply.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f4b2476-8f93-4d4e-b664-233e82fcf212.jpg?1792591213"
    }
}

