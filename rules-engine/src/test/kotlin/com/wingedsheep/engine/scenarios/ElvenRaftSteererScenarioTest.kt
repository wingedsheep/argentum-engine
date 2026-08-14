package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Elven Raft-Steerer (HOB #37) — {2}{U} Creature — Elf Pilot 3/2.
 *
 * "Landfall — Whenever a land you control enters, choose one —
 *  • Tap target creature an opponent controls.
 *  • Untap target creature you control."
 *
 * A modal *trigger*, so the mode choice happens at resolution rather than at cast. Each mode is
 * driven end to end, and each mode's target filter is checked to point at the right side.
 */
class ElvenRaftSteererScenarioTest : ScenarioTestBase() {

    init {
        context("Elven Raft-Steerer") {

            test("landfall mode 0 taps a creature an opponent controls") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elven Raft-Steerer")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val theirBears = game.findPermanent("Grizzly Bears")!!
                game.state.getEntity(theirBears)?.has<TappedComponent>() shouldBe false

                game.execute(
                    PlayLand(game.player1Id, game.findCardsInHand(1, "Island").single())
                ).error shouldBe null
                game.resolveStack()

                val modeDecision = game.getPendingDecision()
                withClue("the landfall trigger asks which mode to take") {
                    (modeDecision is ChooseOptionDecision) shouldBe true
                }
                game.submitDecision(OptionChosenResponse(modeDecision!!.id, 0)).error shouldBe null

                val targetDecision = game.getPendingDecision()
                withClue("mode 0 then asks for its target") {
                    (targetDecision is ChooseTargetsDecision) shouldBe true
                }
                withClue("only the opponent's creature is offered") {
                    val legal = (targetDecision as ChooseTargetsDecision).legalTargets[0].orEmpty()
                    legal shouldContain theirBears
                    legal shouldNotContain game.findPermanent("Elven Raft-Steerer")!!
                }
                game.selectTargets(listOf(theirBears)).error shouldBe null
                game.resolveStack()

                withClue("the opponent's creature is tapped") {
                    game.state.getEntity(theirBears)?.has<TappedComponent>() shouldBe true
                }
            }

            test("landfall mode 1 untaps a creature you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elven Raft-Steerer")
                    .withCardOnBattlefield(1, "Centaur Courser", tapped = true)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myCourser = game.findPermanent("Centaur Courser")!!
                val theirBears = game.findPermanent("Grizzly Bears")!!
                game.state.getEntity(myCourser)?.has<TappedComponent>() shouldBe true

                game.execute(
                    PlayLand(game.player1Id, game.findCardsInHand(1, "Island").single())
                ).error shouldBe null
                game.resolveStack()

                val modeDecision = game.getPendingDecision()!!
                game.submitDecision(OptionChosenResponse(modeDecision.id, 1)).error shouldBe null

                val targetDecision = game.getPendingDecision() as ChooseTargetsDecision
                withClue("mode 1 offers your creatures, not the opponent's") {
                    val legal = targetDecision.legalTargets[0].orEmpty()
                    legal shouldContain myCourser
                    legal shouldNotContain theirBears
                }
                game.selectTargets(listOf(myCourser)).error shouldBe null
                game.resolveStack()

                withClue("your tapped creature is untapped") {
                    game.state.getEntity(myCourser)?.has<TappedComponent>() shouldBe false
                }
            }

            test("an opponent's land entering does not trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elven Raft-Steerer")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    PlayLand(game.player2Id, game.findCardsInHand(2, "Island").single())
                ).error shouldBe null
                game.resolveStack()

                withClue("the trigger is scoped to lands *you* control") {
                    game.hasPendingDecision() shouldBe false
                    game.state.getEntity(game.findPermanent("Grizzly Bears")!!)
                        ?.has<TappedComponent>() shouldBe false
                }
            }
        }
    }
}
