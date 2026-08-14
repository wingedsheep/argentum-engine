package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * The Spot, Living Portal (SPM #153) — {3}{W}{B} Legendary Creature — Human Scientist Villain 4/4.
 *
 * "When The Spot enters, exile up to one target nonland permanent and up to one target nonland
 *  permanent card from a graveyard.
 *  When The Spot dies, put him on the bottom of his owner's library. If you do, return the exiled
 *  cards to their owners' hands."
 *
 * Exercises:
 *  - the two-independent-optional-target ETB linked exile (`ExileUntilLeaves` twice into one
 *    `LinkedExileComponent`): a battlefield nonland permanent AND a nonland permanent card in a
 *    graveyard, and
 *  - the dies clause: `IfYouDo(PutOnBottomOfLibrary(Self), ReturnLinkedExileToHand())` — The Spot
 *    tucks itself to the bottom of its owner's library and, because it did, returns both exiled
 *    cards to their owners' hands.
 */
class TheSpotLivingPortalScenarioTest : ScenarioTestBase() {

    init {
        context("The Spot, Living Portal — ETB two-target linked exile + dies tuck-and-return") {

            test("exiles a battlefield permanent and a graveyard card; dying bottoms The Spot and returns both to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "The Spot, Living Portal")
                    .withCardInHand(1, "Murder")
                    // {3}{W}{B} for The Spot, then {1}{B}{B} for Murder.
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    // The opponent's battlefield nonland permanent (ETB target 0).
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    // The nonland permanent card in the opponent's graveyard (ETB target 1).
                    .withCardInGraveyard(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentBears = game.findPermanent("Grizzly Bears")!!
                val graveyardCourser = game.state.getGraveyard(game.player2Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Centaur Courser"
                }

                // Cast The Spot; its ETB trigger pauses to choose the two targets.
                val cast = game.castSpell(1, "The Spot, Living Portal")
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }

                var guard = 0
                while (game.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
                    game.resolveStack(); guard++
                }
                val td = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected ChooseTargetsDecision for the ETB exile; got ${game.state.pendingDecision}")
                game.submitDecision(
                    TargetsResponse(
                        td.id,
                        mapOf(0 to listOf(opponentBears), 1 to listOf(graveyardCourser)),
                    )
                )
                game.resolveStack()

                withClue("The Spot resolved onto the battlefield") {
                    game.isOnBattlefield("The Spot, Living Portal") shouldBe true
                }
                withClue("the battlefield permanent was exiled by the ETB") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                    game.isInHand(2, "Grizzly Bears") shouldBe false
                }
                withClue("the graveyard card was exiled by the ETB") {
                    game.isInGraveyard(2, "Centaur Courser") shouldBe false
                    game.isInHand(2, "Centaur Courser") shouldBe false
                }

                // Kill The Spot (Murder destroys any creature — The Spot is a 4/4 black creature).
                val theSpot = game.findPermanent("The Spot, Living Portal")!!
                val kill = game.castSpell(1, "Murder", theSpot)
                withClue("Murder cast should succeed: ${kill.error}") { kill.error shouldBe null }
                game.resolveStack() // resolve Murder -> The Spot dies -> dies trigger on stack
                game.resolveStack() // resolve the dies trigger

                withClue("The Spot is on the bottom of its owner's library, not in the graveyard") {
                    game.isInGraveyard(1, "The Spot, Living Portal") shouldBe false
                    val library = game.state.getLibrary(game.player1Id)
                    val bottom = library.lastOrNull()?.let { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name
                    }
                    bottom shouldBe "The Spot, Living Portal"
                }
                withClue("both exiled cards return to their owner's hand") {
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInHand(2, "Centaur Courser") shouldBe true
                }
            }

            test("both ETB targets are optional — declining exiles nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "The Spot, Living Portal")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "The Spot, Living Portal")
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }

                var guard = 0
                while (game.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
                    game.resolveStack(); guard++
                }
                val td = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected ChooseTargetsDecision for the ETB exile; got ${game.state.pendingDecision}")
                // "up to one" — decline both slots.
                game.submitDecision(TargetsResponse(td.id, mapOf(0 to emptyList(), 1 to emptyList())))
                game.resolveStack()

                withClue("nothing was exiled") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Centaur Courser") shouldBe true
                }
            }
        }
    }
}
