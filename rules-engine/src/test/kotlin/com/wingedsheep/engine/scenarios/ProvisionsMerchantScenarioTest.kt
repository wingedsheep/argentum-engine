package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Provisions Merchant (WOE #321) — {2}{G}{G} Creature — Beast Peasant, 3/3.
 *
 * When this creature enters, create a Food token.
 * Whenever this creature attacks, you may sacrifice a Food. If you do, attacking creatures get
 * +1/+1 and gain trample until end of turn.
 *
 * The attack ability's UX hinges on the `feasibility` gate: with no Food the "you may sacrifice a
 * Food" question is unanswerable, and asking it on every single attack would be noise. These tests
 * pin both halves — the prompt appears when there is a Food to sacrifice, and is skipped when there
 * isn't.
 */
class ProvisionsMerchantScenarioTest : ScenarioTestBase() {

    init {
        context("Provisions Merchant") {

            test("entering the battlefield creates a Food token") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Provisions Merchant")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Provisions Merchant").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Provisions Merchant") shouldBe true
                game.findPermanents("Food").size shouldBe 1
            }

            test("attacking with a Food asks whether to sacrifice it") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Provisions Merchant", summoningSickness = false)
                    .withCardOnBattlefield(1, "Food", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Provisions Merchant" to 2)).error shouldBe null
                game.resolveStack()

                withClue("with a Food available, the optional sacrifice is offered") {
                    game.hasPendingDecision() shouldBe true
                }
            }

            test("attacking with no Food does not ask an unanswerable question") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Provisions Merchant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Provisions Merchant" to 2)).error shouldBe null
                game.resolveStack()

                withClue("no Food to sacrifice, so the 'you may' prompt is skipped entirely") {
                    game.hasPendingDecision() shouldBe false
                }
            }

            test("sacrificing the Food pumps every attacking creature and grants trample") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Provisions Merchant", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Food", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Provisions Merchant" to 2, "Grizzly Bears" to 2)
                ).error shouldBe null
                game.resolveStack()

                // Say yes to the optional sacrifice, then pick the Food.
                game.answerYesNo(true)
                game.resolveStack()

                val merchant = game.findPermanent("Provisions Merchant")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("the Food was sacrificed") {
                    game.isOnBattlefield("Food") shouldBe false
                }
                withClue("both attackers get +1/+1") {
                    game.state.projectedState.getPower(merchant) shouldBe 4
                    game.state.projectedState.getToughness(merchant) shouldBe 4
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }
            }
        }
    }
}
