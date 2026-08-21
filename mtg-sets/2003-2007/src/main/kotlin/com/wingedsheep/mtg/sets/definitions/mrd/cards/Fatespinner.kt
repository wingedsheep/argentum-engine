package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fatespinner — Mirrodin #36 (canonical printing)
 * {1}{U}{U} · Creature — Human Wizard · Rare · 1/2
 *
 * At the beginning of each opponent's upkeep, that player chooses draw step, main phase, or
 * combat phase. The player skips each instance of the chosen step or phase this turn.
 *
 * Modelling notes:
 * - **The opponent chooses, and the opponent is skipped.** Both halves are
 *   [Player.TriggeringPlayer] — the player whose upkeep it is. `ChooseActionEffect` already routes
 *   its decision to whatever `player` resolves to, so nothing new was needed for the routing; a
 *   step trigger stores the active player in the context's *entity* slot, which
 *   `TargetResolutionUtils` already falls back to.
 * - **"Each instance ... this turn" is a duration, not a count**, which is why this uses
 *   [Effects.SkipStepOrPhaseThisTurn] rather than the one-shot `SkipNextDrawStep` /
 *   `SkipCombatPhases` markers. Choosing "main phase" skips *both* main phases (CR 505.1 — the
 *   precombat and postcombat main phases are individually and collectively the main phase), and
 *   choosing "combat phase" also swallows an additional combat phase created later that turn.
 * - The trigger resolves during the upkeep, so nothing it names has started yet and all three
 *   options are live (CR 614.10 — a step already under way can no longer be skipped).
 * - The choice is mandatory and always has three feasible options, so the decision is always
 *   presented; there is no "choose nothing" branch.
 */
val Fatespinner = card("Fatespinner") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 2
    oracleText = "At the beginning of each opponent's upkeep, that player chooses draw step, " +
        "main phase, or combat phase. The player skips each instance of the chosen step or " +
        "phase this turn."

    triggeredAbility {
        trigger = Triggers.EachOpponentUpkeep
        effect = Effects.ChooseAction(
            player = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            choices = listOf(
                EffectChoice(
                    label = "Draw step",
                    effect = Effects.SkipStepOrPhaseThisTurn(
                        TurnPart.DRAW_STEP,
                        EffectTarget.PlayerRef(Player.TriggeringPlayer)
                    )
                ),
                EffectChoice(
                    label = "Main phase",
                    effect = Effects.SkipStepOrPhaseThisTurn(
                        TurnPart.MAIN_PHASE,
                        EffectTarget.PlayerRef(Player.TriggeringPlayer)
                    )
                ),
                EffectChoice(
                    label = "Combat phase",
                    effect = Effects.SkipStepOrPhaseThisTurn(
                        TurnPart.COMBAT_PHASE,
                        EffectTarget.PlayerRef(Player.TriggeringPlayer)
                    )
                )
            )
        )
        description = "At the beginning of each opponent's upkeep, that player chooses draw " +
            "step, main phase, or combat phase. The player skips each instance of the chosen " +
            "step or phase this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "rk post"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/5780f4e0-dcfb-4d1f-8ae1-762b98970abb.jpg"
    }
}
