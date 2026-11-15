package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Arashin Foremost (DTK #3) — {1}{W}{W} 2/2 Human Warrior, double strike.
 *
 * "Whenever this creature enters or attacks, another target Warrior creature you control gains
 * double strike until end of turn."
 *
 * Pins the target set (another Warrior *you* control — not the Foremost, not a non-Warrior, not an
 * opponent's Warrior) and that both the enters and the attack trigger grant double strike.
 */
class ArashinForemostScenarioTest : ScenarioTestBase() {

    init {
        context("Arashin Foremost") {

            test("entering grants double strike to another Warrior you control, and only that is targetable") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Arashin Foremost")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(1, "Dromoka Warrior")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser") // a Warrior, but the opponent's
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val warrior = game.findPermanent("Dromoka Warrior")!!

                game.castSpell(1, "Arashin Foremost").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("only another Warrior you control is a legal target") {
                    decision.legalTargets[0] shouldBe listOf(warrior)
                }
                game.selectTargets(listOf(warrior)).error shouldBe null
                game.resolveStack()

                game.state.projectedState.hasKeyword(warrior, Keyword.DOUBLE_STRIKE) shouldBe true
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe false
            }

            test("attacking grants double strike to another Warrior you control") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Arashin Foremost", summoningSickness = false)
                    .withCardOnBattlefield(1, "Dromoka Warrior")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val warrior = game.findPermanent("Dromoka Warrior")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Arashin Foremost" to 2)).error shouldBe null
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(warrior)).error shouldBe null
                game.resolveStack()

                game.state.projectedState.hasKeyword(warrior, Keyword.DOUBLE_STRIKE) shouldBe true
            }

            test("with no other Warrior the enters trigger has no target and does nothing") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Arashin Foremost")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Arashin Foremost").error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isOnBattlefield("Arashin Foremost") shouldBe true
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe false
            }
        }
    }
}
