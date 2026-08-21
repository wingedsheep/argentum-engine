package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Deep-Cavern Bat (LCI #102) — {1}{B} Creature — Bat, 1/1, flying, lifelink.
 *
 * "When this creature enters, look at target opponent's hand. You may exile a nonland card from it
 *  until this creature leaves the battlefield."
 *
 * The exile half is a linked pipeline exile; the LTB trigger returns the linked pile to its owner's
 * hand. These tests pin both halves — in particular that killing the bat gives the card back to the
 * opponent who owned it.
 */
class DeepCavernBatScenarioTest : ScenarioTestBase() {

    init {
        context("Deep-Cavern Bat") {

            test("ETB exiles a chosen nonland card; killing the bat returns it to its owner's hand") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Deep-Cavern Bat")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInHand(2, "Hill Giant")
                    .withCardInHand(2, "Forest")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Deep-Cavern Bat")
                game.resolveStack()
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                val giantId = game.findCardsInHand(2, "Hill Giant").first()
                game.selectCards(listOf(giantId))

                withClue("the chosen nonland card is exiled") {
                    game.isInHand(2, "Hill Giant") shouldBe false
                    game.state.getExile(game.player2Id).any { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Hill Giant"
                    } shouldBe true
                }
                withClue("the land stays in the opponent's hand") {
                    game.isInHand(2, "Forest") shouldBe true
                }

                val batId = game.findPermanent("Deep-Cavern Bat")!!
                val linked = game.state.getEntity(batId)?.get<LinkedExileComponent>()
                linked shouldNotBe null
                linked!!.exiledIds shouldHaveSize 1
                linked.exiledIds.first() shouldBe giantId

                game.castSpell(1, "Murder", batId).error shouldBe null
                game.resolveStack()

                withClue("the bat died") {
                    game.isOnBattlefield("Deep-Cavern Bat") shouldBe false
                    game.isInGraveyard(1, "Deep-Cavern Bat") shouldBe true
                }
                withClue("the exiled card goes back to its OWNER's hand") {
                    game.isInHand(2, "Hill Giant") shouldBe true
                    game.isInHand(1, "Hill Giant") shouldBe false
                    game.state.getExile(game.player2Id) shouldHaveSize 0
                }
            }

            test("dying in combat on a later turn still returns the exiled card") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Deep-Cavern Bat")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(2, "Hill Giant")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Deep-Cavern Bat")
                game.resolveStack()
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()
                val giantId = game.findCardsInHand(2, "Hill Giant").first()
                game.selectCards(listOf(giantId))
                game.isInHand(2, "Hill Giant") shouldBe false

                // Hand the turn over; the opponent attacks and the 1/1 bat blocks the 2/2 Bears.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Deep-Cavern Bat" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("the bat died in combat") {
                    game.isOnBattlefield("Deep-Cavern Bat") shouldBe false
                    game.isInGraveyard(1, "Deep-Cavern Bat") shouldBe true
                }
                withClue("the exiled card goes back to its OWNER's hand") {
                    game.isInHand(2, "Hill Giant") shouldBe true
                    game.state.getExile(game.player2Id) shouldHaveSize 0
                }
            }

            test("killed with its ETB trigger still on the stack, nothing is exiled") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Deep-Cavern Bat")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Murder")
                    .withCardInHand(2, "Hill Giant")
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Deep-Cavern Bat")
                game.passPriority()
                game.passPriority() // the bat resolves; its ETB trigger goes on the stack
                val batId = game.findPermanent("Deep-Cavern Bat")!!

                // The opponent kills the bat while its ETB trigger is still on the stack.
                game.passPriority()
                game.castSpell(2, "Murder", batId).error shouldBe null
                game.resolveStack()

                withClue("the bat died before its trigger resolved") {
                    game.isOnBattlefield("Deep-Cavern Bat") shouldBe false
                    game.isInGraveyard(1, "Deep-Cavern Bat") shouldBe true
                }
                withClue("the trigger still looks at the hand, but exiles nothing (LCI ruling 2023-11-10)") {
                    game.state.pendingDecision shouldBe null
                    game.isInHand(2, "Hill Giant") shouldBe true
                    game.state.getExile(game.player2Id) shouldHaveSize 0
                }
            }
        }
    }
}
