package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Merchant Ship — covers the new
 * [com.wingedsheep.sdk.scripting.EventPattern.BecomesUnblockedEvent] trigger detection
 * (CR 509.3g), mapped behind [com.wingedsheep.sdk.dsl.Triggers.AttacksAndIsntBlocked].
 *
 * Oracle:
 *  - This creature can't attack unless defending player controls an Island.
 *  - Whenever this creature attacks and isn't blocked, you gain 2 life.
 *  - When you control no Islands, sacrifice this creature.
 *
 * The unblocked trigger fires once per attacker that has no blockers assigned at the end of
 * the Declare Blockers step. The matcher reads from [BlockersDeclaredEvent]: if Merchant
 * Ship is attacking and its id never appears in any blocker's blocked-attackers list, it
 * triggers exactly once.
 */
class MerchantShipScenarioTest : ScenarioTestBase() {

    init {
        context("Merchant Ship — attacks and isn't blocked → +2 life") {

            test("gains 2 life when it attacks unblocked") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Merchant Ship", summoningSickness = false)
                    // Defender controls an Island so the attack restriction is satisfied.
                    .withLandsOnBattlefield(2, "Island", 1)
                    // Controller keeps an Island so the "no Islands" state trigger doesn't
                    // sacrifice the ship before combat resolves.
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .build()

                val startLife = game.getLifeTotal(1)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Merchant Ship" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                // Defender declines to block — Merchant Ship is unblocked.
                game.declareBlockers(emptyMap()).error shouldBe null
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Merchant Ship unblocked should gain controller 2 life") {
                    game.getLifeTotal(1) shouldBe startLife + 2
                }
            }

            test("does not gain life when Merchant Ship is blocked") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Merchant Ship", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withActivePlayer(1)
                    .build()

                val startLife = game.getLifeTotal(1)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Merchant Ship" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Merchant Ship"))).error shouldBe null
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Merchant Ship was blocked → no life gain") {
                    game.getLifeTotal(1) shouldBe startLife
                }
            }
        }
    }
}
