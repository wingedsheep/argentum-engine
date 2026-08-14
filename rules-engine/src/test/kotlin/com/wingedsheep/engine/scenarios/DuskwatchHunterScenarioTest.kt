package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Duskwatch Hunter (HOB) — {2}{B/G} Creature — Wolf 3/1.
 *
 * "This creature can't be blocked by tokens.
 *  When this creature enters, put a +1/+1 counter on target creature."
 *
 * The evasion has to distinguish a token blocker from an otherwise identical nontoken one, and the
 * ETB counter is a real counter on any creature — the Hunter itself included.
 */
class DuskwatchHunterScenarioTest : ScenarioTestBase() {

    init {
        context("Duskwatch Hunter") {

            test("its ETB puts a +1/+1 counter on a chosen creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Duskwatch Hunter")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!

                game.castSpell(1, "Duskwatch Hunter").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                game.selectTargets(listOf(courser)).error shouldBe null
                game.resolveStack()

                withClue("a permanent +1/+1 counter, not a temporary pump") {
                    game.state.getEntity(courser)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                    game.state.projectedState.getPower(courser) shouldBe 4
                    game.state.projectedState.getToughness(courser) shouldBe 4
                }
            }

            test("the ETB may target the Hunter itself, making it a 4/2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Duskwatch Hunter")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Duskwatch Hunter").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val hunter = game.findPermanent("Duskwatch Hunter")!!
                (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                game.selectTargets(listOf(hunter)).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(hunter) shouldBe 4
                game.state.projectedState.getToughness(hunter) shouldBe 2
            }

            test("a token can't block it, but the same creature as a nontoken can") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Duskwatch Hunter")
                    .withCardOnBattlefield(2, "Grizzly Bears", isToken = true)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Duskwatch Hunter" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("the Bears is a token, so it can't block the Hunter") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Duskwatch Hunter")))
                        .error shouldNotBe null
                }
                withClue("a nontoken creature blocks it just fine") {
                    game.declareBlockers(mapOf("Centaur Courser" to listOf("Duskwatch Hunter")))
                        .error shouldBe null
                }
            }
        }
    }
}
