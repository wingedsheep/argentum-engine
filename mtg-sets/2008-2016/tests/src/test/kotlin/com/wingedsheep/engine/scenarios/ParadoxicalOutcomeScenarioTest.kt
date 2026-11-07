package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Paradoxical Outcome (KLD #60) — "Return any number of target nonland, nontoken permanents you
 * control to their owners' hands. Draw a card for each card returned to your hand this way."
 *
 * The draw count is the returned cards narrowed to those that landed in *your* hand, so a
 * permanent you control but don't own goes home to its owner and draws nothing (2021-03-19 ruling).
 */
class ParadoxicalOutcomeScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Glory Seeker")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInHand(1, "Paradoxical Outcome")
            .withLandsOnBattlefield(1, "Island", 4)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.cast(targets: List<EntityId>) {
            val spell = findCardsInHand(1, "Paradoxical Outcome").single()
            val result = execute(
                CastSpell(
                    playerId = player1Id,
                    cardId = spell,
                    targets = targets.map { entityIdToChosenTarget(state, it) },
                )
            )
            withClue("casting with ${targets.size} targets: ${result.error}") { result.error shouldBe null }
            resolveStack()
        }

        test("returns each targeted permanent and draws a card for each") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val seeker = game.findPermanent("Glory Seeker")!!
            val handBefore = game.handSize(1) - 1 // the spell itself leaves the hand

            game.cast(listOf(bears, seeker))

            withClue("both permanents are back in P1's hand") {
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Glory Seeker") shouldBe true
            }
            withClue("two returned + two drawn") {
                game.handSize(1) shouldBe handBefore + 4
            }
        }

        test("a permanent you control but don't own returns to its owner and draws nothing") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.state = game.state.updateEntity(giant) { it.with(ControllerComponent(game.player1Id)) }
            val handBefore = game.handSize(1) - 1
            val opponentHandBefore = game.handSize(2)

            game.cast(listOf(bears, giant))

            withClue("the borrowed Hill Giant goes to P2's hand, not P1's") {
                game.isInHand(2, "Hill Giant") shouldBe true
                game.isInHand(1, "Hill Giant") shouldBe false
                game.handSize(2) shouldBe opponentHandBefore + 1
            }
            withClue("only Grizzly Bears came back to P1's hand: one returned + one drawn") {
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe handBefore + 2
            }
        }

        test("cast with no targets, it resolves and draws nothing") {
            val game = board()
            val handBefore = game.handSize(1) - 1

            game.cast(emptyList())

            game.handSize(1) shouldBe handBefore
            game.isInHand(1, "Grizzly Bears") shouldBe false
            game.isInHand(1, "Glory Seeker") shouldBe false
        }
    }
}
