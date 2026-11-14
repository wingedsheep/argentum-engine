package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tainted Remedy (ORI #120) — "If an opponent would gain life, that player loses that much life
 * instead."
 *
 * Proves the replacement is scoped to the enchantment controller's opponents, that the loss lands
 * on the player who would have gained (not on Tainted Remedy's controller), and the ruling that the
 * gaining player orders it against their own Alhammarret's Archive.
 */
class TaintedRemedyScenarioTest : ScenarioTestBase() {

    /** Player 1 controls Tainted Remedy; [caster] casts Soothing Balm ("target player gains 5 life"). */
    private fun board(caster: Int, withArchive: Boolean = false): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Tainted Remedy")
            .withCardInHand(caster, "Soothing Balm")
            .withLandsOnBattlefield(caster, "Plains", 2)
            .withActivePlayer(caster)
            .withPriorityPlayer(caster)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (withArchive) builder.withCardOnBattlefield(2, "Alhammarret's Archive")
        return builder.build()
    }

    init {
        context("Tainted Remedy") {

            test("an opponent who would gain life loses that much life instead") {
                val game = board(caster = 2)
                game.castSpellTargetingPlayer(2, "Soothing Balm", 2).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 15
                game.getLifeTotal(1) shouldBe 20
            }

            test("its controller gains life normally, and the loss follows the opponent it is aimed at") {
                val own = board(caster = 1)
                own.castSpellTargetingPlayer(1, "Soothing Balm", 1).error shouldBe null
                own.resolveStack()
                own.getLifeTotal(1) shouldBe 25

                val aimed = board(caster = 1)
                aimed.castSpellTargetingPlayer(1, "Soothing Balm", 2).error shouldBe null
                aimed.resolveStack()
                aimed.getLifeTotal(2) shouldBe 15
                aimed.getLifeTotal(1) shouldBe 20
            }

            for ((first, expectedLife) in listOf("Alhammarret's Archive" to 10, "Tainted Remedy" to 15)) {
                test("the gaining player applies $first first against their own Alhammarret's Archive") {
                    val game = board(caster = 2, withArchive = true)
                    game.castSpellTargetingPlayer(2, "Soothing Balm", 2).error shouldBe null
                    game.resolveStack()

                    val decision = game.state.pendingDecision as ChooseOptionDecision
                    decision.playerId shouldBe game.player2Id
                    val index = decision.options.indexOfFirst { it.startsWith(first) }
                    withClue("options: ${decision.options}") { (index >= 0) shouldBe true }
                    game.submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
                    game.resolveStack()

                    game.getLifeTotal(2) shouldBe expectedLife
                    game.state.pendingDecision shouldBe null
                }
            }
        }
    }
}
