package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Anointer of Valor (CMR #8) — {5}{W} Creature — Angel, 3/5.
 *
 *   Flying
 *   Whenever a creature attacks, you may pay {3}. When you do, put a +1/+1 counter on that
 *   creature.
 *
 * The trigger watches every attacker on either side; the {3} is paid by Anointer's controller and
 * the reflexive ability puts the counter on the attacking creature. Covers Anointer attacking
 * itself, an opponent's creature attacking (Anointer's controller still pays, the opponent's
 * creature gets the counter), and declining the payment.
 */
class AnointerOfValorScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.untappedPlains(playerId: EntityId): Int =
        state.getBattlefield(playerId).count { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == "Plains" &&
                state.getEntity(id)?.get<TappedComponent>() == null
        }

    /** Answer the attack trigger's "pay {3}?" with [pay], auto-tapping mana, until the stack settles. */
    private fun TestGame.runAttackTrigger(pay: Boolean): Int {
        var prompts = 0
        var guard = 0
        while (guard++ < 40) {
            when (val decision = state.pendingDecision) {
                is YesNoDecision -> {
                    withClue("Anointer's controller is the one asked to pay") {
                        decision.playerId shouldBe player1Id
                    }
                    prompts++
                    answerYesNo(pay)
                }
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay()
                null -> {
                    if (state.stack.isEmpty()) return prompts
                    resolveStack()
                }
                else -> error("unexpected decision: $decision")
            }
        }
        error("decision loop did not settle")
    }

    init {
        context("Anointer of Valor") {

            test("Anointer attacking itself: paying {3} puts a +1/+1 counter on it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Anointer of Valor", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val anointer = game.findPermanent("Anointer of Valor")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Anointer of Valor" to 2)).error shouldBe null
                game.resolveStack()

                withClue("one attacker, one pay prompt") {
                    game.runAttackTrigger(pay = true) shouldBe 1
                }
                withClue("Anointer has a +1/+1 counter") {
                    game.plusOneCounters(anointer) shouldBe 1
                }
                withClue("the {3} was paid") {
                    game.untappedPlains(game.player1Id) shouldBe 0
                }
                game.state.projectedState.getPower(anointer) shouldBe 4
                game.state.projectedState.getToughness(anointer) shouldBe 6
            }

            test("an opponent's attacking creature gets the counter when Anointer's controller pays") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Anointer of Valor")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val anointer = game.findPermanent("Anointer of Valor")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.resolveStack()

                game.runAttackTrigger(pay = true) shouldBe 1

                withClue("the opponent's attacking Grizzly Bears got the counter") {
                    game.plusOneCounters(bears) shouldBe 1
                }
                withClue("Anointer itself got nothing") {
                    game.plusOneCounters(anointer) shouldBe 0
                }
                withClue("Anointer's controller paid the {3}") {
                    game.untappedPlains(game.player1Id) shouldBe 0
                }
            }

            test("declining the {3} puts no counter on the attacker and taps no mana") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Anointer of Valor", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val anointer = game.findPermanent("Anointer of Valor")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Anointer of Valor" to 2)).error shouldBe null
                game.resolveStack()

                game.runAttackTrigger(pay = false) shouldBe 1

                withClue("no counter was placed") {
                    game.plusOneCounters(anointer) shouldBe 0
                }
                withClue("no mana was spent") {
                    game.untappedPlains(game.player1Id) shouldBe 3
                }
            }
        }
    }
}
