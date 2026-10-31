package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SuppressZoneChangeTriggers

/**
 * Karn, Argent Defender — Reality Fracture #279
 * {2} · Legendary Artifact Creature — Golem · 1/3 · Rare
 *
 * Artifacts and creatures entering the battlefield don't cause abilities to trigger.
 *
 * Doorkeeper Thrull's lock on a colorless body: [SuppressZoneChangeTriggers] widened from its
 * creature-only default to [GameObjectFilter.CreatureOrArtifact]. The entering permanent is matched
 * in projected state, replacement effects ("enters tapped", "enters with counters", "as this
 * enters") are untouched, and permanents entering alongside Karn are suppressed too. Karn is itself
 * an artifact creature, so its own entry causes no triggers either.
 */
val KarnArgentDefender = card("Karn, Argent Defender") {
    manaCost = "{2}"
    typeLine = "Legendary Artifact Creature — Golem"
    power = 1
    toughness = 3
    oracleText = "Artifacts and creatures entering the battlefield don't cause abilities to trigger."

    staticAbility {
        ability = SuppressZoneChangeTriggers(GameObjectFilter.CreatureOrArtifact)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "279"
        artist = "Scott M. Fischer"
        flavorText = "\"I owe the Multiverse a debt I fear I can never repay. That remedy begins now, " +
            "with a first step away from the past and toward the future I wish to see.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1ebbbddb-2dc3-4194-b72b-13bcebe2ab89.jpg?1788878301"
        inBooster = false
    }
}
