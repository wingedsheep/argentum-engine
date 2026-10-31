package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mythos of Vadrok (IKO #127) — {2}{R}{R} Sorcery.
 *
 * "Mythos of Vadrok deals 5 damage divided as you choose among any number of target creatures
 *  and/or planeswalkers. If {W}{U} was spent to cast this spell, until your next turn, those
 *  permanents can't attack or block and their activated abilities can't be activated."
 *
 * The opponent controls two Hill Giants (3/3) and a Centaur Courser (3/3). The spell divides 4 to
 * one Giant and 1 to the other, leaving the Courser untargeted. Paying with Plains + Island spends
 * {W}{U}, which locks down the surviving target only; paying with four Mountains does not.
 */
class MythosOfVadrokScenarioTest : ScenarioTestBase() {

    private fun scenarioWith(vararg lands: Pair<String, Int>) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Mythos of Vadrok")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Centaur Courser")
        .apply { lands.forEach { (land, n) -> withLandsOnBattlefield(1, land, n) } }
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("with {W}{U} spent, the divided damage lands and the surviving target is locked down") {
            val game = scenarioWith("Mountain" to 2, "Plains" to 1, "Island" to 1)
            val spell = game.findCardsInHand(1, "Mythos of Vadrok").single()
            val (doomed, survivor) = game.findAllPermanents("Hill Giant")
            val courser = game.findPermanent("Centaur Courser")!!

            game.execute(
                CastSpell(
                    game.player1Id, spell,
                    targets = listOf(ChosenTarget.Permanent(doomed), ChosenTarget.Permanent(survivor)),
                    damageDistribution = mapOf(doomed to 4, survivor to 1),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the Giant assigned 4 dies, the one assigned 1 survives") {
                game.state.getBattlefield().contains(doomed) shouldBe false
                game.state.getBattlefield().contains(survivor) shouldBe true
            }
            withClue("the surviving target can't attack or block") {
                game.state.projectedState.cantAttack(survivor) shouldBe true
                game.state.projectedState.cantBlock(survivor) shouldBe true
            }
            withClue("the untargeted Courser is unaffected") {
                game.state.projectedState.cantAttack(courser) shouldBe false
                game.state.projectedState.cantBlock(courser) shouldBe false
            }
        }

        test("without {W}{U} spent, only the damage happens") {
            val game = scenarioWith("Mountain" to 4)
            val spell = game.findCardsInHand(1, "Mythos of Vadrok").single()
            val (doomed, survivor) = game.findAllPermanents("Hill Giant")

            game.execute(
                CastSpell(
                    game.player1Id, spell,
                    targets = listOf(ChosenTarget.Permanent(doomed), ChosenTarget.Permanent(survivor)),
                    damageDistribution = mapOf(doomed to 4, survivor to 1),
                )
            ).error shouldBe null
            game.resolveStack()

            game.state.getBattlefield().contains(doomed) shouldBe false
            game.state.projectedState.cantAttack(survivor) shouldBe false
            game.state.projectedState.cantBlock(survivor) shouldBe false
        }
    }
}
