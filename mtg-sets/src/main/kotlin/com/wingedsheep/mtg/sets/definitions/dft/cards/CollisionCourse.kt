package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Collision Course
 * {1}{W}
 * Sorcery
 *
 * Choose one —
 * • Collision Course deals X damage to target creature, where X is the number of permanents you
 *   control that are creatures and/or Vehicles.
 * • Destroy target artifact.
 *
 * A true "Choose one" modal spell. Mode 1's X is a [DynamicAmount.Count] over the battlefield
 * filtered to creatures and/or Vehicles you control; mode 2 is a plain destroy.
 */
val CollisionCourse = card("Collision Course") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Collision Course deals X damage to target creature, where X is the number of permanents " +
        "you control that are creatures and/or Vehicles.\n" +
        "• Destroy target artifact."

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: deal X damage to target creature
            Mode(
                effect = Effects.DealDamage(
                    amount = DynamicAmount.Count(
                        Player.You,
                        Zone.BATTLEFIELD,
                        GameObjectFilter.CreatureOrVehicle,
                    ),
                    target = EffectTarget.ContextTarget(0),
                ),
                targetRequirements = listOf(Targets.Creature),
                description = "Deals X damage to target creature, where X is the number of " +
                    "creatures and/or Vehicles you control",
            ),
            // Mode 2: destroy target artifact
            Mode(
                effect = Effects.Destroy(EffectTarget.ContextTarget(0)),
                targetRequirements = listOf(Targets.Artifact),
                description = "Destroy target artifact",
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Konstantin Porubov"
        flavorText = "\"These gators are driving like idiots!\"\n—Redshift"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b60da34-b622-42de-a249-79545bcbf30d.jpg?1782687958"
    }
}
