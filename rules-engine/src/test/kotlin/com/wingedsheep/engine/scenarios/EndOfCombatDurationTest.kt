package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Engine-level tests for [Duration.EndOfCombat].
 *
 * CR 500.5a / 511.2: effects that last "until end of combat" expire when the combat *phase* ends —
 * not when the end of combat step begins. Nothing expired them: `CombatManager.endCombat` cleared
 * the combat components but left `floatingEffects` alone, and end-of-turn cleanup dropped them as a
 * safety net, so "until end of combat" silently behaved as "until end of turn". Anything using the
 * duration was affected — Battering Ram's banding grant, Murk Dwellers' pump.
 *
 * [Duration.EndOfTurn] is the control: the same pump written the printed way must still be there in
 * the postcombat main phase, so these prove an expiry rather than a blanket clear of floating
 * effects.
 */
class EndOfCombatDurationTest : ScenarioTestBase() {

    /** "Target creature gets +2/+0 until end of combat." */
    private val combatPump = card("Combat Surge") {
        manaCost = "{G}"
        typeLine = "Instant"
        colorIdentity = "G"
        oracleText = "Target creature gets +2/+0 until end of combat."
        spell {
            target = Targets.Creature
            effect = Effects.ModifyStats(2, 0, duration = Duration.EndOfCombat)
        }
    }

    /** The same card with the ordinary duration, as a control. */
    private val turnPump = card("Turn Surge") {
        manaCost = "{G}"
        typeLine = "Instant"
        colorIdentity = "G"
        oracleText = "Target creature gets +2/+0 until end of turn."
        spell {
            target = Targets.Creature
            effect = Effects.ModifyStats(2, 0, duration = Duration.EndOfTurn)
        }
    }

    init {
        cardRegistry.register(combatPump)
        cardRegistry.register(turnPump)

        /** Cast [spellName] on a Grizzly Bears in the precombat main phase. */
        fun pumpedBears(spellName: String): Pair<TestGame, EntityId> {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInHand(1, spellName)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, spellName, bears).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getPower(bears) shouldBe 4
            return game to bears
        }

        context("Duration.EndOfCombat") {

            test("survives the end of combat step and expires as the combat phase ends") {
                val (game, bears) = pumpedBears("Combat Surge")

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                withClue("CR 500.5a: it expires at the end of the phase, not when this step begins") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                }

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("the combat phase is over — the bonus is gone") {
                    game.state.projectedState.getPower(bears) shouldBe 2
                }
            }

            test("expires even in a turn where the pump was cast before combat was entered") {
                val (game, bears) = pumpedBears("Combat Surge")

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.state.projectedState.getPower(bears) shouldBe 2
            }

            test("an until-end-of-turn pump is untouched by the same transition") {
                val (game, bears) = pumpedBears("Turn Surge")

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("only EndOfCombat expires here — this must last the whole turn") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                }
            }
        }
    }
}
