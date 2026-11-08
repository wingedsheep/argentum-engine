package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Indomitable Creativity (AER #85) — {X}{R}{R}{R} Sorcery.
 *
 *   "Destroy X target artifacts and/or creatures. For each permanent destroyed this way, its
 *    controller reveals cards from the top of their library until an artifact or creature card is
 *    revealed and exiles that card. Those players put the exiled cards onto the battlefield, then
 *    shuffle."
 *
 * Proves the per-controller tally (each player digs once per permanent of theirs that was
 * destroyed), that an indestructible target counts for nothing (and its controller neither
 * reveals nor shuffles), that a destroyed permanent Rest in Peace exiles still counts, and that a
 * library with no artifact or creature card is revealed whole, nothing enters, and it is shuffled.
 */
class IndomitableCreativityScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.cast(x: Int, targets: List<EntityId>): ExecutionResult {
            val card = state.getHand(player1Id).first {
                state.getEntity(it)?.get<CardComponent>()?.name == "Indomitable Creativity"
            }
            return execute(CastSpell(player1Id, card, targets.map { ChosenTarget.Permanent(it) }, xValue = x))
        }

        fun TestGame.controllerOf(name: String): List<EntityId> =
            findAllPermanents(name).mapNotNull { state.projectedState.getController(it) }

        fun shuffledPlayers(results: List<ExecutionResult>): List<EntityId> =
            results.flatMap { it.events }.filterIsInstance<LibraryShuffledEvent>().map { it.playerId }

        context("Indomitable Creativity") {

            test("each controller digs once per permanent of theirs destroyed; the cards enter under them") {
                val game = scenario().withPlayers()
                    .withCardInHand(1, "Indomitable Creativity")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    // Player 1's library, top first.
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Artifact Creature")
                    .withCardInLibrary(1, "Forest")
                    // Player 2's library, top first.
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Goblin Guide")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Llanowar Elves")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targets = listOf("Grizzly Bears", "Centaur Courser", "Savannah Lions")
                    .map { game.findPermanent(it)!! }
                game.cast(3, targets).error shouldBe null
                val results = game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Centaur Courser") shouldBe true
                game.isInGraveyard(2, "Savannah Lions") shouldBe true

                // Player 1 lost one permanent: one dig, stopping at the first artifact/creature.
                game.controllerOf("Artifact Creature") shouldBe listOf(game.player1Id)
                game.librarySize(1) shouldBe 2

                // Player 2 lost two: two artifact/creature cards, the lands revealed on the way stay.
                game.controllerOf("Goblin Guide") shouldBe listOf(game.player2Id)
                game.controllerOf("Llanowar Elves") shouldBe listOf(game.player2Id)
                game.librarySize(2) shouldBe 3
                game.findCardsInLibrary(2, "Forest").size shouldBe 2
                game.findCardsInLibrary(2, "Mountain").size shouldBe 1

                shuffledPlayers(results) shouldContainExactlyInAnyOrder listOf(game.player1Id, game.player2Id)
            }

            test("an indestructible target isn't destroyed, so its controller neither reveals nor shuffles") {
                val game = scenario().withPlayers()
                    .withCardInHand(1, "Indomitable Creativity")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Darksteel Myr")
                    .withCardInLibrary(1, "Goblin Guide")
                    .withCardInLibrary(2, "Llanowar Elves")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targets = listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Darksteel Myr")!!)
                game.cast(2, targets).error shouldBe null
                val results = game.resolveStack()

                game.isOnBattlefield("Darksteel Myr") shouldBe true
                game.findCardsInLibrary(2, "Llanowar Elves").size shouldBe 1
                game.isOnBattlefield("Llanowar Elves") shouldBe false

                game.controllerOf("Goblin Guide") shouldBe listOf(game.player1Id)
                shuffledPlayers(results) shouldBe listOf(game.player1Id)
            }

            test("a permanent destroyed but exiled by Rest in Peace still counts") {
                val game = scenario().withPlayers()
                    .withCardInHand(1, "Indomitable Creativity")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(1, "Rest in Peace")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Goblin Guide")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cast(1, listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
                val results = game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.controllerOf("Goblin Guide") shouldBe listOf(game.player2Id)
                game.librarySize(2) shouldBe 1
                shuffledPlayers(results) shouldBe listOf(game.player2Id)
            }

            test("a library with no artifact or creature card is revealed whole, nothing enters, and it is shuffled") {
                val game = scenario().withPlayers()
                    .withCardInHand(1, "Indomitable Creativity")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cast(1, listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
                val results = game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.librarySize(2) shouldBe 2
                game.findAllPermanents("Forest").shouldBeEmpty()
                shuffledPlayers(results) shouldBe listOf(game.player2Id)
            }
        }
    }
}
