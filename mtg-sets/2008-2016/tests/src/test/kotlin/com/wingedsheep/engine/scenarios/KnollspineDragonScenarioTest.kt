package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Knollspine Dragon (SHM #98) — "When this creature enters, you may discard your hand and draw
 * cards equal to the damage dealt to target opponent this turn."
 *
 * First card to read the per-player damage-received tracker, so these pin that the count is the
 * damage dealt to the *targeted* opponent this turn and that the "may" is settled on resolution.
 */
class KnollspineDragonScenarioTest : ScenarioTestBase() {

    /** The Dragon, [bolts] Lightning Bolts and two Plains (the hand to discard) in hand. */
    private fun setup(bolts: Int): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Knollspine Dragon")
            .withCardsInHand(1, "Plains", 2)
            .withLandsOnBattlefield(1, "Mountain", 7 + bolts)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(bolts) { builder = builder.withCardInHand(1, "Lightning Bolt") }
        repeat(6) { builder = builder.withCardInLibrary(1, "Island") }
        return builder.build()
    }

    private fun TestGame.bolt() {
        castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
        resolveStack()
    }

    /** Cast the Dragon, point its trigger at the opponent, and answer the "may". */
    private fun TestGame.castDragon(accept: Boolean) {
        castSpell(1, "Knollspine Dragon").error shouldBe null
        resolveStack()
        if (getPendingDecision() is ChooseTargetsDecision) selectTargets(listOf(player2Id)).error shouldBe null
        resolveStack()
        withClue("the discard-and-draw is optional, asked on resolution") {
            getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
        }
        answerYesNo(accept).error shouldBe null
        resolveStack()
    }

    private fun TestGame.handNames(): List<String> =
        state.getHand(player1Id).map { state.getEntity(it)!!.get<CardComponent>()!!.name }

    init {
        context("Knollspine Dragon") {
            test("discards the hand and draws as many cards as damage dealt to the opponent this turn") {
                val game = setup(bolts = 1)

                game.bolt() // 3 damage to the opponent
                game.castDragon(accept = true)

                withClue("three cards drawn from a library of Islands") {
                    game.handNames() shouldBe listOf("Island", "Island", "Island")
                }
                withClue("the two Plains were discarded") {
                    game.state.getGraveyard(game.player1Id).count {
                        game.state.getEntity(it)!!.get<CardComponent>()!!.name == "Plains"
                    } shouldBe 2
                }
            }

            test("counts all damage dealt to that opponent this turn, from every source") {
                val game = setup(bolts = 2)

                game.bolt()
                game.bolt() // 6 damage in total
                game.castDragon(accept = true)

                game.handNames() shouldBe List(6) { "Island" }
            }

            test("declining keeps the hand and draws nothing") {
                val game = setup(bolts = 1)

                game.bolt()
                game.castDragon(accept = false)

                game.handNames().sorted() shouldBe listOf("Plains", "Plains")
            }

            test("with no damage dealt this turn the hand is discarded and nothing is drawn") {
                val game = setup(bolts = 0)

                game.castDragon(accept = true)

                game.handNames() shouldBe emptyList()
            }
        }
    }
}
