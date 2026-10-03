package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Markov Warlord (DKA) — haste; when it enters, up to two target creatures
 * can't block this turn.
 */
class MarkovWarlordScenarioTest : ScenarioTestBase() {

    private fun setup(): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Markov Warlord")
            .withLandsOnBattlefield(1, "Mountain", 6)
            .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
            .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
            .withCardOnBattlefield(2, "Wall of Wood", summoningSickness = false)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(3) { builder = builder.withCardInLibrary(1, "Mountain") }
        repeat(3) { builder = builder.withCardInLibrary(2, "Forest") }
        return builder.build()
    }

    private fun castAndTarget(game: TestGame) {
        val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
        val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
        game.castSpell(1, "Markov Warlord").error shouldBe null
        game.resolveStack()
        game.selectTargets(listOf(bears, giant)).error shouldBe null
        game.resolveStack()
        game.isOnBattlefield("Markov Warlord") shouldBe true
    }

    init {
        context("Markov Warlord") {
            test("both targeted creatures can't block the hasty Warlord this turn") {
                val game = setup()
                castAndTarget(game)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                withClue("haste lets the Warlord attack the turn it enters") {
                    game.declareAttackers(mapOf("Markov Warlord" to 2)).error shouldBe null
                }
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("Grizzly Bears was targeted, so it can't block") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Markov Warlord"))).error shouldNotBe null
                }
                withClue("Hill Giant was targeted, so it can't block") {
                    game.declareBlockers(mapOf("Hill Giant" to listOf("Markov Warlord"))).error shouldNotBe null
                }
            }

            test("an untargeted creature can still block") {
                val game = setup()
                castAndTarget(game)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Markov Warlord" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("Wall of Wood wasn't targeted, so its block is legal") {
                    game.declareBlockers(mapOf("Wall of Wood" to listOf("Markov Warlord"))).error shouldBe null
                }
            }
        }
    }
}
