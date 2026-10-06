package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Paralyze.
 *
 * The clause worth proving is *who* is asked and *who* pays at the upkeep trigger: the Aura's
 * controller owns the ability, but "that player" is the enchanted creature's controller. So the
 * Aura goes on an opponent's creature, and the test checks the yes/no lands on the opponent and
 * the {4} comes out of the opponent's lands.
 */
class ParalyzeScenarioTest : ScenarioTestBase() {

    init {
        context("Paralyze") {

            fun board() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Paralyze")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Swamp", 4)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun tapped(game: TestGame, id: EntityId) =
                game.state.getEntity(id)?.has<TappedComponent>() == true

            test("taps on arrival; the enchanted creature's controller may pay {4} to untap it") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Paralyze", bears).error shouldBe null
                game.resolveStack()
                withClue("the enters trigger taps the enchanted creature") { tapped(game, bears) shouldBe true }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                withClue("its untap step skipped it") { tapped(game, bears) shouldBe true }

                game.resolveStack()
                val decision = game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                withClue("the creature's controller is asked, not the Aura's") {
                    decision.playerId shouldBe game.player2Id
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("paying untaps the creature") { tapped(game, bears) shouldBe false }
                withClue("the {4} came out of Player2's lands") {
                    game.findPermanents("Swamp")
                        .count { tapped(game, it) } shouldBe 4 + 1 // Player2's four, plus Player1's Swamp tapped to cast
                }
            }

            test("declining leaves the creature tapped") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Paralyze", bears).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                tapped(game, bears) shouldBe true
            }
        }
    }
}
