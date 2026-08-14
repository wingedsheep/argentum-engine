package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Lagorin, Soul of Alacria (DFT #211).
 *
 * Lagorin, Soul of Alacria {G}{W} — Legendary Creature — Beast Mount 1/1
 * Flying
 * Whenever Lagorin attacks while saddled, put a +1/+1 counter on each of up to two target Mounts
 * and/or Vehicles.
 * Saddle 1
 *
 * Two claims: the reward is gated on being saddled *when declared as an attacker* (the printed
 * ruling), and its targets are **permanents** — an uncrewed Vehicle is legal even though it isn't a
 * creature, while a plain creature isn't.
 */
class LagorinSoulOfAlacriaScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun saddledAttackGame(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Lagorin, Soul of Alacria")
        .withCardOnBattlefield(1, "Grizzly Bears") // the saddler, and not a legal target itself
        .withCardOnBattlefield(1, "Air Response Unit") // uncrewed Vehicle — a legal target
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Lagorin, Soul of Alacria") {

            test("attacking while saddled offers only Mounts and Vehicles, and counters both") {
                val game = saddledAttackGame()
                val lagorin = game.findPermanent("Lagorin, Soul of Alacria")!!
                val vehicle = game.findPermanent("Air Response Unit")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    com.wingedsheep.engine.core.SaddleMount(game.player1Id, lagorin, listOf(bears))
                ).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Lagorin, Soul of Alacria" to 2)).error shouldBe null

                val decision = game.getPendingDecision() as ChooseTargetsDecision
                withClue("Lagorin is itself a Mount, and the uncrewed Vehicle qualifies") {
                    decision.legalTargets.getValue(0) shouldContain vehicle
                    decision.legalTargets.getValue(0) shouldContain lagorin
                }
                withClue("a plain creature is neither a Mount nor a Vehicle") {
                    decision.legalTargets.getValue(0) shouldNotContain bears
                }

                game.selectTargets(listOf(lagorin, vehicle)).error shouldBe null
                game.resolveStack()

                withClue("one counter on each of the two chosen permanents") {
                    plusOneCounters(game, lagorin) shouldBe 1
                    plusOneCounters(game, vehicle) shouldBe 1
                }
            }

            test("\"up to two\" means one target is a legal answer") {
                val game = saddledAttackGame()
                val lagorin = game.findPermanent("Lagorin, Soul of Alacria")!!
                val vehicle = game.findPermanent("Air Response Unit")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    com.wingedsheep.engine.core.SaddleMount(game.player1Id, lagorin, listOf(bears))
                ).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Lagorin, Soul of Alacria" to 2)).error shouldBe null

                game.selectTargets(listOf(vehicle)).error shouldBe null
                game.resolveStack()

                plusOneCounters(game, vehicle) shouldBe 1
                plusOneCounters(game, lagorin) shouldBe 0
            }

            test("attacking unsaddled does not trigger the reward") {
                val game = saddledAttackGame()
                val lagorin = game.findPermanent("Lagorin, Soul of Alacria")!!
                val vehicle = game.findPermanent("Air Response Unit")!!

                // No saddle this turn — the intervening "if" fails when Lagorin is declared.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Lagorin, Soul of Alacria" to 2)).error shouldBe null

                withClue("no trigger, so nothing asks for targets") {
                    game.hasPendingDecision() shouldBe false
                }
                game.resolveStack()
                plusOneCounters(game, vehicle) shouldBe 0
                plusOneCounters(game, lagorin) shouldBe 0
            }
        }
    }
}
