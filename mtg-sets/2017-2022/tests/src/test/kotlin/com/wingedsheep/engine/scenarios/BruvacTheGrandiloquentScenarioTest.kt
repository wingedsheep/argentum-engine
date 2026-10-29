package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Bruvac the Grandiloquent (JMP #10).
 *
 * {2}{U} Legendary Creature — Human Advisor 1/4
 * If an opponent would mill one or more cards, they mill twice that many cards instead.
 *
 * Exercises the multiplicative mill-amount replacement through the real card: an opponent's mill
 * is doubled, Bruvac's controller's own mill is not, and next to The Water Crystal's "+4" the
 * milling opponent chooses the order (CR 616.1) — 2 becomes 12 or 8.
 */
class BruvacTheGrandiloquentScenarioTest : ScenarioTestBase() {

    init {
        context("Bruvac the Grandiloquent") {

            fun TestGame.activateMillstone(target: EntityId) {
                val ability = cardRegistry.getCard("Millstone")!!.script.activatedAbilities[0]
                val result = execute(
                    ActivateAbility(
                        playerId = player1Id,
                        sourceId = findPermanent("Millstone")!!,
                        abilityId = ability.id,
                        targets = listOf(entityIdToChosenTarget(state, target)),
                    )
                )
                withClue("Activating Millstone should succeed: ${result.error}") { result.error shouldBe null }
                resolveStack()
            }

            fun board(vararg extra: String) = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Bruvac the Grandiloquent")
                .also { b -> extra.forEach { b.withCardOnBattlefield(1, it) } }
                .withCardOnBattlefield(1, "Millstone")
                .withLandsOnBattlefield(1, "Island", 2)
                .also { b -> repeat(15) { b.withCardInLibrary(1, "Plains") } }
                .also { b -> repeat(15) { b.withCardInLibrary(2, "Forest") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("an opponent who would mill 2 mills 4 instead") {
                val game = board()
                game.activateMillstone(game.player2Id)

                game.getPendingDecision() shouldBe null
                game.graveyardSize(2) shouldBe 4
            }

            test("Bruvac's controller mills only the printed number") {
                val game = board()
                game.activateMillstone(game.player1Id)

                game.getPendingDecision() shouldBe null
                game.graveyardSize(1) shouldBe 2
            }

            test("next to The Water Crystal the milling opponent picks the order") {
                val game = board("The Water Crystal")
                game.activateMillstone(game.player2Id)

                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
                decision.playerId shouldBe game.player2Id
                decision.options shouldContainExactly listOf(
                    "Mill 12 (The Water Crystal, then Bruvac the Grandiloquent)",
                    "Mill 8 (Bruvac the Grandiloquent, then The Water Crystal)",
                )
                game.submitDecision(OptionChosenResponse(decision.id, 0)).error shouldBe null

                game.getPendingDecision() shouldBe null
                game.graveyardSize(2) shouldBe 12
            }

            test("the opponent may instead double first and mill 8") {
                val game = board("The Water Crystal")
                game.activateMillstone(game.player2Id)

                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
                game.submitDecision(OptionChosenResponse(decision.id, 1)).error shouldBe null

                game.graveyardSize(2) shouldBe 8
            }
        }
    }
}
