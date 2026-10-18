package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetChooser

/**
 * The Abyss
 * {3}{B}
 * World Enchantment
 *
 * At the beginning of each player's upkeep, destroy target nonartifact creature that player
 * controls of their choice. It can't be regenerated.
 *
 * The target is the ability controller's target, but the upkeep player picks it
 * ([TargetChooser.TriggeringPlayer]) from their own nonartifact creatures — the Quicksilver
 * Fountain shape. The world supertype puts it under the world rule (CR 704.5k).
 */
val TheAbyss = card("The Abyss") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "World Enchantment"
    oracleText = "At the beginning of each player's upkeep, destroy target nonartifact creature that " +
        "player controls of their choice. It can't be regenerated."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.nonartifact().controlledByTriggeringPlayer()),
            chooser = TargetChooser.TriggeringPlayer,
        )
        effect = Effects.Destroy(creature, noRegenerate = true)
        description = "At the beginning of each player's upkeep, destroy target nonartifact creature " +
            "that player controls of their choice. It can't be regenerated."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86a27d68-3e58-4ade-976d-36381beed451.jpg?1783948063"
    }
}
