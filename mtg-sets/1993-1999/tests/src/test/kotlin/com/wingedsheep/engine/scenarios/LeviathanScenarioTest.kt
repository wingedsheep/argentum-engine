package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Leviathan (DRK #30).
 *
 * {5}{U}{U}{U}{U} Creature — Leviathan 10/10
 * "Trample. This creature enters tapped and doesn't untap during your untap step.
 *  At the beginning of your upkeep, you may sacrifice two Islands. If you do, untap this creature.
 *  This creature can't attack unless you sacrifice two Islands."
 *
 * The attack clause is a *cost*, not a condition, so the tests check both halves of that: a
 * declaration with too few Islands is rejected outright, and a legal one stops to ask which Islands
 * to sacrifice and actually consumes them.
 */
class LeviathanScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Leviathan") {

            test("can't be declared as an attacker without two Islands to sacrifice") {
                val game = scenario()
                    .withPlayers("Deep", "Shore")
                    .withCardOnBattlefield(1, "Leviathan")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val result = game.declareAttackers(mapOf("Leviathan" to 2))
                withClue("one Island can't pay a two-Island cost, so the declaration is illegal") {
                    result.error shouldNotBe null
                }
            }

            test("attacking pauses to sacrifice two Islands, and consumes them") {
                val game = scenario()
                    .withPlayers("Deep", "Shore")
                    .withCardOnBattlefield(1, "Leviathan")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val leviathan = game.findPermanent("Leviathan")!!

                game.declareAttackers(mapOf("Leviathan" to 2)).error shouldBe null

                val decision = game.state.pendingDecision
                decision.shouldNotBeNull()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("all three Islands are eligible, and exactly two must go") {
                    decision.options.size shouldBe 3
                    decision.minSelections shouldBe 2
                    decision.maxSelections shouldBe 2
                }

                game.submitDecision(CardsSelectedResponse(decision.id, decision.options.take(2)))

                withClue("two Islands were sacrificed, leaving one") {
                    game.findPermanents("Island").size shouldBe 1
                }
                withClue("and the Leviathan is attacking") {
                    game.findPermanent("Leviathan").shouldNotBeNull()
                    projector.project(game.state).getPower(leviathan) shouldBe 10
                }
            }
        }
    }
}
