package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GiveControlToTargetPlayerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Jinxed Idol
 * {2}
 * Artifact
 *
 * At the beginning of your upkeep, this artifact deals 2 damage to you.
 * Sacrifice a creature: Target opponent gains control of this artifact.
 */
val JinxedIdol = card("Jinxed Idol") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "At the beginning of your upkeep, this artifact deals 2 damage to you.\n" +
        "Sacrifice a creature: Target opponent gains control of this artifact."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.DealDamage(2, EffectTarget.Controller)
    }

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Creature)
        val opponent = target("opponent", TargetOpponent())
        effect = GiveControlToTargetPlayerEffect(
            permanent = EffectTarget.Self,
            newController = opponent
        )
        description = "Sacrifice a creature: Target opponent gains control of this artifact."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "293"
        artist = "John Matson"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c728e38-5656-4feb-8610-0cf45fb38094.jpg?1562052774"
    }
}
