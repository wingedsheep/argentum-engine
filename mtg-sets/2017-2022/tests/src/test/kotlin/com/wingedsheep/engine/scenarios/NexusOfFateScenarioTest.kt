package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Nexus of Fate (M19 #306) — {5}{U}{U} Instant.
 *
 *   Take an extra turn after this one.
 *   If Nexus of Fate would be put into a graveyard from anywhere, reveal Nexus of Fate and shuffle
 *   it into its owner's library instead.
 *
 * Ruling (2018-07-13): the last ability applies "in any way, including while it's resolving" — the
 * resolution case is the one that used to land the card in the library without the shuffle.
 */
class NexusOfFateScenarioTest : ScenarioTestBase() {

    private fun TestGame.nexusInLibrary(): Boolean =
        state.getLibrary(player1Id).any { state.getEntity(it)?.get<CardComponent>()?.name == "Nexus of Fate" }

    init {
        test("resolving grants an extra turn and shuffles Nexus into its owner's library") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Nexus of Fate")
                .withLandsOnBattlefield(1, "Island", 7)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val librarySizeBefore = game.librarySize(1)

            game.castSpell(1, "Nexus of Fate").error shouldBe null
            val events = game.resolveStack().flatMap { it.events }

            withClue("player 1 takes the extra turn, so player 2 skips their next turn") {
                game.state.getEntity(game.player2Id)?.has<SkipNextTurnComponent>() shouldBe true
            }
            withClue("the graveyard destination of a resolving spell is replaced by the library") {
                game.isInGraveyard(1, "Nexus of Fate") shouldBe false
                game.nexusInLibrary() shouldBe true
                game.librarySize(1) shouldBe librarySizeBefore + 1
            }
            withClue("it is shuffled in, not placed on top or bottom") {
                events.count { it is LibraryShuffledEvent && it.playerId == game.player1Id } shouldBe 1
            }
        }

        test("countered, Nexus is shuffled into its owner's library and no extra turn is taken") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Nexus of Fate")
                .withLandsOnBattlefield(1, "Island", 7)
                .withCardInHand(2, "Cancel")
                .withLandsOnBattlefield(2, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Nexus of Fate").error shouldBe null
            game.passPriority().error shouldBe null
            game.castSpellTargetingStackSpell(2, "Cancel", "Nexus of Fate").error shouldBe null
            game.resolveStack()

            withClue("no extra turn — the spell never resolved") {
                game.state.getEntity(game.player2Id)?.has<SkipNextTurnComponent>() shouldBe false
            }
            withClue("Nexus went to its owner's library instead of the graveyard") {
                game.isInGraveyard(1, "Nexus of Fate") shouldBe false
                game.nexusInLibrary() shouldBe true
                game.isInGraveyard(2, "Cancel") shouldBe true
            }
        }
    }
}
