package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldNotBe

/**
 * Commit // Memory (AKH #211).
 *
 * Commit {3}{U}: "Put target spell or nonland permanent into its owner's library second from the top."
 *
 * Commit is the first card to move a *spell* off the stack with a positional library move, so both
 * halves of its union target are exercised: an opponent's creature spell lands second from the top
 * without resolving, a nonland permanent does the same, and a land can't be targeted. Memory is
 * Timetwister's body behind aftermath, both already covered elsewhere.
 */
class CommitMemoryScenarioTest : ScenarioTestBase() {

    private fun TestGame.castCommit(target: ChosenTarget) =
        execute(CastSpell(player1Id, findCardsInHand(1, "Commit // Memory").first(), listOf(target), faceIndex = 0))

    private fun TestGame.libraryNames(playerNumber: Int): List<String?> =
        state.getLibrary(if (playerNumber == 1) player1Id else player2Id)
            .map { state.getEntity(it)?.get<CardComponent>()?.name }

    init {
        test("an opponent's spell goes into its owner's library second from the top without resolving") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Commit // Memory")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val topBefore = game.libraryNames(2)
            game.castSpell(2, "Grizzly Bears").error.shouldBeNull()
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.passPriority()

            val bears: EntityId = game.state.stack.first { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
            }
            game.castCommit(ChosenTarget.Spell(bears)).error.shouldBeNull()
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("the creature spell never resolved") {
                game.findPermanent("Grizzly Bears").shouldBeNull()
            }
            withClue("the Bears sit second from the top of their owner's library") {
                game.libraryNames(2) shouldContainExactly listOf(topBefore[0], "Grizzly Bears", topBefore[1])
            }
        }

        test("a nonland permanent goes into its owner's library second from the top") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Commit // Memory")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val topBefore = game.libraryNames(2)
            val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
            game.castCommit(ChosenTarget.Permanent(giant)).error.shouldBeNull()
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.findPermanent("Hill Giant").shouldBeNull()
            game.libraryNames(2) shouldContainExactly listOf(topBefore[0], "Hill Giant", topBefore[1])
        }

        test("a land can't be targeted") {
            val game = scenario()
                .withPlayers("Caster", "Opponent")
                .withCardInHand(1, "Commit // Memory")
                .withLandsOnBattlefield(1, "Island", 4)
                .withLandsOnBattlefield(2, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val forest = game.findPermanent("Forest").shouldNotBeNull()
            game.castCommit(ChosenTarget.Permanent(forest)).error shouldNotBe null
        }
    }
}
