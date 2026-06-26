package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Sandworm (FIN #155).
 *
 * "{4}{R} Creature — Worm 5/4. Haste. When this creature enters, destroy target land. Its controller
 *  may search their library for a basic land card, put it onto the battlefield tapped, then shuffle."
 *
 * Verifies the ETB destroys the targeted land, and that the optional basic-land search is performed
 * by the *destroyed land's controller* (the opponent here), who may accept or decline.
 */
class SandwormScenarioTest : ScenarioTestBase() {

    init {
        context("Sandworm") {

            test("ETB destroys target land; its controller may fetch a tapped basic land") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardInHand(1, "Sandworm")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val victimPlains = game.state.getBattlefield().first { id ->
                    val e = game.state.getEntity(id)
                    e?.get<CardComponent>()?.name == "Plains" &&
                        e.get<ControllerComponent>()?.playerId == game.player2Id
                }

                game.castSpell(1, "Sandworm")
                game.resolveStack()

                // ETB trigger needs a land target — choose the opponent's Plains.
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(victimPlains))
                    game.resolveStack()
                }

                // Its controller (player 2) may search — accept.
                game.hasPendingDecision() shouldBe true
                game.answerYesNo(true)
                game.resolveStack()

                // Select the Forest from player 2's library.
                if (game.hasPendingDecision()) {
                    val forest = game.state.getLibrary(game.player2Id).first { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Forest"
                    }
                    game.selectCards(listOf(forest))
                    game.resolveStack()
                }

                withClue("The targeted Plains should be destroyed") {
                    game.state.getBattlefield().none { id ->
                        val e = game.state.getEntity(id)
                        e?.get<CardComponent>()?.name == "Plains" &&
                            e.get<ControllerComponent>()?.playerId == game.player2Id
                    } shouldBe true
                }
                val forestOnBf = game.state.getBattlefield().firstOrNull { id ->
                    val e = game.state.getEntity(id)
                    e?.get<CardComponent>()?.name == "Forest" &&
                        e.get<ControllerComponent>()?.playerId == game.player2Id
                }
                withClue("Player 2 should have fetched a Forest onto the battlefield") {
                    (forestOnBf != null) shouldBe true
                }
                withClue("The fetched Forest should be tapped") {
                    game.state.getEntity(forestOnBf!!)?.get<TappedComponent>() shouldBe TappedComponent
                }
            }

            test("the land's controller may decline the search") {
                val game = scenario()
                    .withPlayers("Caster", "Victim")
                    .withCardInHand(1, "Sandworm")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withLandsOnBattlefield(2, "Plains", 1)
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val victimPlains = game.state.getBattlefield().first { id ->
                    val e = game.state.getEntity(id)
                    e?.get<CardComponent>()?.name == "Plains" &&
                        e.get<ControllerComponent>()?.playerId == game.player2Id
                }

                game.castSpell(1, "Sandworm")
                game.resolveStack()

                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(victimPlains))
                    game.resolveStack()
                }

                game.hasPendingDecision() shouldBe true
                game.answerYesNo(false)
                game.resolveStack()

                withClue("The targeted Plains should still be destroyed") {
                    game.state.getBattlefield().none { id ->
                        val e = game.state.getEntity(id)
                        e?.get<CardComponent>()?.name == "Plains" &&
                            e.get<ControllerComponent>()?.playerId == game.player2Id
                    } shouldBe true
                }
                withClue("No Forest should have entered the battlefield") {
                    game.state.getBattlefield().none { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Forest"
                    } shouldBe true
                }
            }
        }
    }
}
