package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Heartfire Hero {R}
 * Creature — Mouse Soldier
 * 1/1
 *
 * Valiant — Whenever this creature becomes the target of a spell or ability
 * you control for the first time each turn, put a +1/+1 counter on it.
 * When this creature dies, it deals damage equal to its power to each opponent.
 */
val HeartfireHero = card("Heartfire Hero") {
    manaCost = "{R}"
    typeLine = "Creature — Mouse Soldier"
    power = 1
    toughness = 1
    oracleText = "Valiant — Whenever this creature becomes the target of a spell or ability you control for the first time each turn, put a +1/+1 counter on it.\nWhen this creature dies, it deals damage equal to its power to each opponent."

    // Valiant — put a +1/+1 counter on it
    triggeredAbility {
        trigger = Triggers.Valiant
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    // When this creature dies, deal damage equal to its power to each opponent
    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.DealDamage(
            DynamicAmounts.sourcePower(),
            EffectTarget.PlayerRef(Player.EachOpponent)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Jakub Kasper"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48ace959-66b2-40c8-9bff-fd7ed9c99a82.jpg?1762883283"
    }
}
