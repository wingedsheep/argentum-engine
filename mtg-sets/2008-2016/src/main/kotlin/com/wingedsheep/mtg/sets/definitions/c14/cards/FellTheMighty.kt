package com.wingedsheep.mtg.sets.definitions.c14.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fell the Mighty
 * {4}{W}
 * Sorcery
 * Destroy all creatures with power greater than target creature's power.
 *
 * A board wipe over `Creature.powerGreaterThanEntity(target)`: the comparison reads the target's
 * projected power as the spell resolves (2014-11-07 ruling), the target itself is never destroyed
 * (its power isn't greater than its own), and the destroyed creatures aren't targeted, so hexproof
 * or protection doesn't save them. An illegal target means the spell doesn't resolve at all.
 */
val FellTheMighty = card("Fell the Mighty") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Destroy all creatures with power greater than target creature's power."

    spell {
        val reference = target(TargetFilter.Creature)
        effect = Effects.DestroyAll(GameObjectFilter.Creature.powerGreaterThanEntity(reference))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Raymond Swanland"
        flavorText = "\"They had it coming.\"\n—Squire Imalia, savior of Thiswick"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4e999d3-c2d7-47dc-81ad-a2baf6cc4757.jpg?1783938873"
        ruling(
            "2014-11-07",
            "Use the power of the target creature as Fell the Mighty resolves to determine which creatures " +
                "are destroyed. Fell the Mighty doesn't target any of those creatures, so a creature with " +
                "hexproof or protection from white will be destroyed if its power is high enough."
        )
        ruling("2014-11-07", "Fell the Mighty won't destroy the target creature.")
        ruling(
            "2014-11-07",
            "If the target creature is an illegal target when Fell the Mighty tries to resolve, Fell the " +
                "Mighty won't resolve and none of its effects will happen. No creatures will be destroyed."
        )
    }
}
