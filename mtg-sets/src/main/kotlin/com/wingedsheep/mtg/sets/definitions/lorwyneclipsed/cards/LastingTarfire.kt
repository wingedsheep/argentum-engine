package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lasting Tarfire
 * {1}{R}
 * Enchantment
 *
 * At the beginning of each end step, if you put a counter on a creature this turn,
 * this enchantment deals 2 damage to each opponent.
 */
val LastingTarfire = card("Lasting Tarfire") {
    manaCost = "{1}{R}"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each end step, if you put a counter on a creature this turn, " +
        "this enchantment deals 2 damage to each opponent."

    triggeredAbility {
        trigger = Triggers.EachEndStep
        triggerCondition = Conditions.PutCounterOnCreatureThisTurn
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Jorge Jacinto"
        flavorText = "What began as a rivalry between two petty marauders ended in a field soaked in tar, soot, and flame."
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c4a95ac-072f-4219-80a0-1ce71f1b8411.jpg?1767732759"
        ruling(
            "2025-11-17",
            "Lasting Tarfire's ability will check as your end step starts to see if you put a counter on a creature this turn. If you didn't, the ability won't trigger at all. Putting a counter on a creature during your end step won't cause the ability to trigger."
        )
    }
}
