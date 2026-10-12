package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for The Phasing of Zhalfir (DMU #59).
 *
 *   Read ahead
 *   I, II — Another target nonland permanent phases out. It can't phase in for as long as you
 *           control this Saga.
 *   III — Destroy all creatures. For each creature destroyed this way, its controller creates a
 *         2/2 black Phyrexian creature token.
 *
 * The lock is not Oubliette's "until this leaves": losing the Saga releases the permanent to its
 * controller's *next untap step*, not immediately (2022-09-09 ruling).
 */
class ThePhasingOfZhalfirScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.castAndChoose(chapter: Int) {
            castSpell(1, "The Phasing of Zhalfir").error shouldBe null
            resolveStack()
            getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
            chooseNumber(chapter)
        }

        fun TestGame.phaseOutWithChapterOne(target: EntityId) {
            castAndChoose(1)
            selectTargets(listOf(target)).error shouldBe null
            resolveStack()
        }

        fun TestGame.isPhasedIn(id: EntityId) = id in state.getBattlefield()

        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "The Phasing of Zhalfir")
            .withLandsOnBattlefield(1, "Island", 4)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }

        test("chapter I: the permanent stays phased out through its controller's untap step") {
            val game = builder().build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.phaseOutWithChapterOne(bears)
            game.isPhasedIn(bears) shouldBe false

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("it can't phase in while you control the Saga") {
                game.isPhasedIn(bears) shouldBe false
            }
        }

        test("losing the Saga releases the permanent at its controller's next untap, not immediately") {
            val game = builder()
                .withCardInHand(1, "Disenchant")
                .withLandsOnBattlefield(1, "Plains", 2)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.phaseOutWithChapterOne(bears)
            val saga = game.findPermanent("The Phasing of Zhalfir")!!
            game.castSpell(1, "Disenchant", saga).error shouldBe null
            game.resolveStack()
            game.findPermanent("The Phasing of Zhalfir") shouldBe null

            withClue("unlike Oubliette, the Saga leaving doesn't phase it in") {
                game.isPhasedIn(bears) shouldBe false
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("it phases in during its controller's next untap step") {
                game.isPhasedIn(bears) shouldBe true
            }
        }

        test("losing control of the Saga releases the lock for good") {
            val game = builder()
                .withCardInHand(2, "Confiscate")
                .withLandsOnBattlefield(2, "Island", 6)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.phaseOutWithChapterOne(bears)
            val saga = game.findPermanent("The Phasing of Zhalfir")!!

            // Opponent's turn: the Bears stay out past the untap step, then the opponent steals
            // the Saga.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player2Id
            game.isPhasedIn(bears) shouldBe false
            game.castSpell(2, "Confiscate", saga).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getController(saga) shouldBe game.player2Id
            game.isPhasedIn(bears) shouldBe false

            // Through my turn into the opponent's next untap step.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.passUntilPhase(Phase.ENDING, Step.END)
            withClue("a stolen Saga gets its lore counter on its controller's turn, not its owner's") {
                game.state.getEntity(saga)!!.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 1
            }
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("you no longer control the Saga, so it phases in at its controller's untap") {
                game.isPhasedIn(bears) shouldBe true
            }
        }

        test("chapter III: destroys all creatures and each controller creates a Phyrexian per creature lost") {
            val game = builder()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(1, "Savannah Lions")
                .build()

            game.castAndChoose(3)
            game.resolveStack()

            game.findPermanents("Grizzly Bears").size shouldBe 0
            game.findPermanent("Savannah Lions") shouldBe null
            val tokens = game.findPermanents("Phyrexian Token")
            withClue("two tokens for the opponent's two Bears, one for my Lions") {
                tokens.count { game.state.projectedState.getController(it) == game.player2Id } shouldBe 2
                tokens.count { game.state.projectedState.getController(it) == game.player1Id } shouldBe 1
            }
            tokens.forEach {
                game.state.projectedState.getPower(it) shouldBe 2
                game.state.projectedState.getToughness(it) shouldBe 2
            }
            withClue("the Saga is sacrificed after chapter III") {
                game.findPermanent("The Phasing of Zhalfir") shouldBe null
            }
        }
    }
}
