package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reset — "after their upkeep step" is every step of an opponent's turn that follows the upkeep
 * (draw step through cleanup); the untap is the Wilderness Reclamation shape.
 */
val Reset = card("Reset") {
    manaCost = "{U}{U}"
    typeLine = "Instant"
    oracleText = "Cast this spell only during an opponent's turn after their upkeep step.\nUntap all lands you control."
    spell {
        castOnlyIf(
            Conditions.All(
                Conditions.IsOpponentsTurn,
                Conditions.IsInStep(
                    Step.DRAW,
                    Step.PRECOMBAT_MAIN,
                    Step.BEGIN_COMBAT,
                    Step.DECLARE_ATTACKERS,
                    Step.DECLARE_BLOCKERS,
                    Step.FIRST_STRIKE_COMBAT_DAMAGE,
                    Step.COMBAT_DAMAGE,
                    Step.END_COMBAT,
                    Step.POSTCOMBAT_MAIN,
                    Step.END,
                    Step.CLEANUP,
                    yoursOnly = false,
                ),
            )
        )
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Land.youControl()),
            Effects.Untap(EffectTarget.IterationEntity)
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Nicola Leonard"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c829d83-d5b8-4be7-80f7-55b42f52b309.jpg?1783948073"
    }
}
