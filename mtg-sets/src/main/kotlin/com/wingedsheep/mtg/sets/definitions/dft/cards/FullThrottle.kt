package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Full Throttle — Aetherdrift #127
 * {4}{R}{R} · Sorcery
 *
 * The additional combats are gated on being in either player's main phase: the spell's ruling says
 * the untap trigger is still installed when it resolves outside a main phase, but no combats are
 * added. The repeating delayed trigger expires at cleanup and untaps only surviving permanents that
 * attacked earlier in the turn.
 */
val FullThrottle = card("Full Throttle") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "After this main phase, there are two additional combat phases.\n" +
        "At the beginning of each combat this turn, untap all creatures that attacked this turn."

    spell {
        effect = Effects.Composite(
            ConditionalEffect(
                condition = Conditions.IsInPhase(
                    Phase.PRECOMBAT_MAIN,
                    Phase.POSTCOMBAT_MAIN,
                    yoursOnly = false,
                ),
                effect = Effects.Composite(Effects.AddCombatPhase, Effects.AddCombatPhase),
            ),
            CreateDelayedTriggerEffect(
                step = Step.BEGIN_COMBAT,
                effect = Patterns.Group.untapGroup(
                    GroupFilter(GameObjectFilter.Creature.attackedThisTurn())
                ),
                repeatAtEachMatchingStep = true,
                expiry = DelayedTriggerExpiry.EndOfTurn,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "127"
        artist = "Benjamin Ee"
        flavorText = "\"You wanted a fight. Now you've got two.\"\n—Chandra Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d91f7cad-89e8-45cb-a78e-b35b0ee64783.jpg?1783907882"

        ruling(
            "2025-02-07",
            "There is no main phase between the two additional combat phases. If you cast this " +
                "during your second main phase, you will go directly to the end step after the two " +
                "additional combat phases are complete."
        )
        ruling(
            "2025-02-07",
            "If you somehow cast this spell when it's not a main phase, the second ability still " +
                "takes effect, but there are no additional combat phases this turn. If you cast it " +
                "during an opponent's main phase, there are two additional combat phases, but that " +
                "opponent gets to attack during those combat phases, not you."
        )
    }
}
