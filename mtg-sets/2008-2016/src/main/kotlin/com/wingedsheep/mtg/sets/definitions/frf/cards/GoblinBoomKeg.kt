package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Goblin Boom Keg
 * {4}
 * Artifact
 * At the beginning of your upkeep, sacrifice this artifact.
 * When this artifact is put into a graveyard from the battlefield, it deals 3 damage to any target.
 */
val GoblinBoomKeg = card("Goblin Boom Keg") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "At the beginning of your upkeep, sacrifice this artifact.\n" +
        "When this artifact is put into a graveyard from the battlefield, it deals 3 damage to any target."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = SacrificeSelfEffect
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(3, anyTarget)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Viktor Titov"
        flavorText = "Dragonfire spawned many imitations. Some were more dramatic than others."
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14343e23-83fb-4d2e-a272-20d7787ef2ca.jpg"
    }
}
