package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Creeping Bloodsucker
 * {1}{B}
 * Creature — Vampire
 * 1/2
 * At the beginning of your upkeep, this creature deals 1 damage to each opponent. You gain life
 * equal to the damage dealt this way.
 *
 * The gain reads the damage actually dealt (`damageDealtVariable`), not a fixed count of
 * opponents: prevented damage gains nothing, and the trigger still resolves fully if the
 * Bloodsucker has left the battlefield by then (the damage comes from its last known information).
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
        effect = Effects.Pipeline {
            val dealt = runStoringNumber {
                Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent), damageDealtVariable = it)
            }
            run(Effects.GainLife(dealt.amount))
        }
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
