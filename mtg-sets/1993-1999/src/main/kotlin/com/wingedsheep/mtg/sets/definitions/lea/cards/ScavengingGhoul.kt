package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scavenging Ghoul
 * {3}{B}
 * Creature — Zombie
 * 2/2
 * At the beginning of each end step, put a corpse counter on this creature for each creature that
 * died this turn.
 * Remove a corpse counter from this creature: Regenerate this creature.
 *
 * "Each creature that died this turn" is global — every player's creatures, tokens included.
 */
val ScavengingGhoul = card("Scavenging Ghoul") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 2
    oracleText = "At the beginning of each end step, put a corpse counter on this creature for each creature that died this turn.\n" +
        "Remove a corpse counter from this creature: Regenerate this creature."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        effect = Effects.AddDynamicCounters(
            CounterType.CORPSE,
            DynamicAmounts.creaturesDiedThisTurn(Player.Each),
            EffectTarget.Self,
        )
        description = "At the beginning of each end step, put a corpse counter on this creature for each creature that died this turn."
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.CORPSE, 1)
        effect = Effects.Regenerate(EffectTarget.Self)
        description = "Remove a corpse counter from this creature: Regenerate this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Jeff A. Menges"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/426984e0-88e1-4a2d-9a1c-798b95864df3.jpg?1783948691"
    }
}
