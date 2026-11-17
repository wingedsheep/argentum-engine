package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Yasova Dragonclaw (FRF #148) — {2}{G} Legendary Creature — Human Warrior, 4/2.
 *
 *   Trample
 *   At the beginning of combat on your turn, you may pay {1}{U/R}{U/R}. If you do, gain control of
 *   target creature an opponent controls with power less than Yasova Dragonclaw's power until end of
 *   turn, untap that creature, and it gains haste until end of turn.
 *
 * Covers the strict power comparison against Yasova on the stack, the steal/untap/haste behind the
 * hybrid may-pay gate, declining the payment, and the ruling that the power comparison is rechecked
 * as the ability resolves.
 */
class YasovaDragonclawScenarioTest : ScenarioTestBase() {

    init {
        context("Yasova Dragonclaw") {

            test("paying steals a lesser-power opposing creature, untapped and hasty, until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Yasova Dragonclaw")
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true) // 2/2 — legal
                    .withCardOnBattlefield(2, "Craw Wurm")                    // 6/4 — power too high
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                withClue("only the opposing creature with power less than Yasova's 4 is a legal target") {
                    decision.legalTargets[0]!! shouldContainExactly listOf(bears)
                }
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("the trigger offers the optional {1}{U/R}{U/R} payment on resolution") {
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true)
                game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
                game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Player1 controls the Bears, untapped and with haste") {
                    game.state.projectedState.getController(bears) shouldBe game.player1Id
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("control returns to Player2 when the turn ends") {
                    game.state.projectedState.getController(bears) shouldBe game.player2Id
                }
            }

            test("declining the payment leaves the target with its owner") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Yasova Dragonclaw")
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                if (game.getPendingDecision() is ChooseTargetsDecision) {
                    game.selectTargets(listOf(bears))
                }
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("nothing is stolen or untapped when the payment is declined") {
                    game.state.projectedState.getController(bears) shouldBe game.player2Id
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                }
            }

            test("a target whose power is no longer less than Yasova's on resolution is illegal") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Yasova Dragonclaw")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(bears))

                // In response, pump the Bears to 5/5 — no longer less than Yasova's 4.
                game.castSpell(1, "Giant Growth", bears).error shouldBe null
                game.resolveStack()

                withClue("the ability fizzles on resolution: no payment is offered") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe false
                }
                withClue("Player2 keeps the Bears") {
                    game.state.projectedState.getController(bears) shouldBe game.player2Id
                }
            }
        }
    }
}
