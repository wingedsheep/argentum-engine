package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hired Claw
 * {R}
 * Creature — Lizard Mercenary
 * 1/2
 *
 * Whenever you attack with one or more Lizards, this creature deals 1 damage to target opponent.
 * {1}{R}: Put a +1/+1 counter on this creature. Activate only if an opponent lost life this turn
 * and only once each turn.
 */
val HiredClaw = card("Hired Claw") {
    manaCost = "{R}"
    typeLine = "Creature — Lizard Mercenary"
    oracleText = "Whenever you attack with one or more Lizards, this creature deals 1 damage to target opponent.\n{1}{R}: Put a +1/+1 counter on this creature. Activate only if an opponent lost life this turn and only once each turn."
    power = 1
    toughness = 2

    triggeredAbility {
        trigger = Triggers.YouAttackWithFilter(GameObjectFilter.Creature.withSubtype(Subtype.LIZARD))
        val opponent = target("opponent", Targets.Opponent)
        effect = Effects.DealDamage(1, opponent)
    }

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.OnlyIfCondition(Conditions.OpponentLostLifeThisTurn),
                ActivationRestriction.OncePerTurn
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "140"
        artist = "Quintin Gleim"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1ae41080-0d67-4719-adb2-49bf2a268b6c.jpg?1721426641"
    }
}
