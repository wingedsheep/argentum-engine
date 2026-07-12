package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Estwald Shieldbasher (VOW #11) — {3}{W} Creature — Human Soldier, 4/2.
 *
 *   Whenever this creature attacks, you may pay {1}. If you do, it gains indestructible until
 *   end of turn.
 *
 * Exercises the optional-mana attack trigger: paying {1} grants indestructible to the attacker;
 * declining leaves it without indestructible.
 */
class EstwaldShieldbasherScenarioTest : ScenarioTestBase() {

    init {
        context("Estwald Shieldbasher — may pay {1} on attack") {

            test("paying {1} grants indestructible until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Estwald Shieldbasher", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shieldbasher = game.findPermanent("Estwald Shieldbasher")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Estwald Shieldbasher" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the attack trigger offers a yes/no to pay {1}") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true)

                withClue("paying yes then prompts for mana sources") {
                    game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
                }
                game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Estwald Shieldbasher gains indestructible") {
                    game.state.projectedState.hasKeyword(shieldbasher, Keyword.INDESTRUCTIBLE) shouldBe true
                }
            }

            test("declining the payment leaves it without indestructible") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Estwald Shieldbasher", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shieldbasher = game.findPermanent("Estwald Shieldbasher")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Estwald Shieldbasher" to 2)).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("declining leaves it without indestructible") {
                    game.state.projectedState.hasKeyword(shieldbasher, Keyword.INDESTRUCTIBLE) shouldBe false
                }
            }
        }
    }
}
