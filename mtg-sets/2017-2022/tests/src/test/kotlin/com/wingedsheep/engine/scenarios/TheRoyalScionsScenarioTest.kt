package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.eld.cards.TheRoyalScions
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The Royal Scions (ELD #199).
 *
 *   −8: Draw four cards. When you do, The Royal Scions deals damage to any target equal to the
 *   number of cards in your hand.
 *
 * Pins the gated reflexive trigger: the target is chosen only after the four cards are in hand,
 * the damage reads the hand size, and — per the ruling — drawing fewer than four cards (here
 * capped by an opposing Narset, Parter of Veils) means the reflexive ability never triggers.
 */
class TheRoyalScionsScenarioTest : ScenarioTestBase() {

    private val minusEight = TheRoyalScions.activatedAbilities[2].id

    private fun setup(opponentNarset: Boolean): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "The Royal Scions")
            .withCardInHand(1, "Mountain")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (opponentNarset) builder.withCardOnBattlefield(2, "Narset, Parter of Veils")
        val game = builder.build()
        val scions = game.findPermanent("The Royal Scions")!!
        game.state = game.state.updateEntity(scions) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, 8))
        }
        return game
    }

    init {
        test("−8: after drawing four, deals damage equal to cards in hand to any target") {
            val game = setup(opponentNarset = false)
            val scions = game.findPermanent("The Royal Scions")!!

            game.execute(ActivateAbility(game.player1Id, scions, minusEight)).error shouldBe null
            game.resolveStack()

            withClue("the reflexive trigger asks for its target after the four cards are drawn") {
                game.handSize(1) shouldBe 5
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            withClue("five cards in hand → 5 damage") {
                game.getLifeTotal(2) shouldBe 15
            }
        }

        test("−8: drawing fewer than four cards means the reflexive ability doesn't trigger") {
            val game = setup(opponentNarset = true)
            val scions = game.findPermanent("The Royal Scions")!!

            game.execute(ActivateAbility(game.player1Id, scions, minusEight)).error shouldBe null
            game.resolveStack()

            withClue("Narset caps the draw at one card") {
                game.handSize(1) shouldBe 2
            }
            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
