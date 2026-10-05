package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Creeping Bloodsucker
 * {1}{B}
 * Creature — Vampire
 * 1/2
 *
 * At the beginning of your upkeep, this creature deals 1 damage to each opponent. You gain life
 * equal to the damage dealt this way.
 *
 * "Damage dealt this way" is the actual damage, so prevented damage gains nothing: the trigger
 * snapshots this creature's damage-dealt-this-turn tally, deals the damage, and gains the
 * difference (the Brightflame shape) — 1 per opponent actually damaged.
 */
val CreepingBloodsucker = card("Creeping Bloodsucker") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 1
    toughness = 2
    oracleText = "At the beginning of your upkeep, this creature deals 1 damage to each opponent. " +
        "You gain life equal to the damage dealt this way."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val damageDealt = DynamicAmounts.propertyOf(EffectTarget.Self, EntityNumericProperty.DamageDealtThisTurn)
        effect = Effects.Pipeline {
            val before = storeNumber(damageDealt)
            run(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)))
            run(Effects.GainLife(damageDealt - before.amount))
        }
        description = "At the beginning of your upkeep, this creature deals 1 damage to each " +
            "opponent. You gain life equal to the damage dealt this way."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Antonio José Manzanedo"
        flavorText = "\"If a vampire were feeding on you every night, wouldn't you have bite marks? " +
            "Stop wasting my time.\"\n—Donya, village healer"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5e3a9e1-fafe-4859-a550-7ae09804aced.jpg?1783919188"
    }
}
