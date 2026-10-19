package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Thundering Raiju (NEO #166) — {2}{R}{R} Creature — Spirit, 3/3, haste.
 *
 * "Whenever this creature attacks, put a +1/+1 counter on target creature you control. Then this
 *  creature deals X damage to each opponent, where X is the number of modified creatures you control
 *  other than this creature."
 *
 * Covers the ordering (the counter lands before X is counted, so the creature that just got it
 * counts) and the "other than this creature" exclusion (countering the Raiju itself adds nothing).
 */
class ThunderingRaijuScenarioTest : ScenarioTestBase() {

    init {
        fun board() = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, "Thundering Raiju")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("Thundering Raiju attack trigger") {

            test("countering another creature makes it modified before X is counted") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Thundering Raiju" to 2)).error shouldBe null
                game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears got the counter and is the one modified creature counted") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("countering the Raiju itself deals no damage: it is excluded from X") {
                val game = board()
                val raiju = game.findPermanent("Thundering Raiju")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Thundering Raiju" to 2)).error shouldBe null
                game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(raiju)).error shouldBe null
                game.resolveStack()

                withClue("only the Raiju is modified, and it doesn't count itself") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
