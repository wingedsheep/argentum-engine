package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.CantSearchLibrariesComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Engine test for a library search performed by a player other than the effect's controller —
 * "Its controller may search their library" (Erode, Sandworm) and "Target player … searches their
 * library" (Restorative Technique). The player who searches is the one whose library it is, so:
 *
 * - a can't-search-libraries restriction (Shadow of Doubt's `CantSearchLibrariesComponent`) is
 *   checked on *that* player, not on the caster — whichever of the two carries it;
 * - "whenever a player searches their library" triggers (Wan Shi Tong, Librarian: "whenever an
 *   opponent searches their library") see the search as that player's.
 *
 * Shadow of Doubt itself stops every player at once, so the restriction is stamped on one player
 * directly to tell the two apart.
 */
class OtherPlayerLibrarySearchScenarioTest : ScenarioTestBase() {

    private fun erodeGame(vararg extra: (ScenarioBuilder) -> ScenarioBuilder): TestGame {
        var builder = scenario()
            .withPlayers("Caster", "Victim")
            .withCardInHand(1, "Erode")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        extra.forEach { builder = it(builder) }
        return builder.build()
    }

    private fun TestGame.forbidSearching(playerId: EntityId) {
        state = state.updateEntity(playerId) { it.with(CantSearchLibrariesComponent()) }
    }

    private fun TestGame.forestsControlledBy(playerId: EntityId): Int =
        state.getBattlefield().count { id ->
            val e = state.getEntity(id)
            e?.get<CardComponent>()?.name == "Forest" && e.get<ControllerComponent>()?.playerId == playerId
        }

    /** Accept the may-search, pick the Forest, and let any trigger resolve. */
    private fun TestGame.searchForForest(searcher: EntityId) {
        if (hasPendingDecision()) {
            answerYesNo(true)
            resolveStack()
        }
        if (hasPendingDecision()) {
            val forest = state.getLibrary(searcher).first { id ->
                state.getEntity(id)?.get<CardComponent>()?.name == "Forest"
            }
            selectCards(listOf(forest))
        }
        resolveStack()
    }

    private fun TestGame.plusOneCounters(name: String): Int =
        state.getEntity(findPermanent(name)!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("the searching player can't search libraries: their search finds nothing") {
            val game = erodeGame()
            game.forbidSearching(game.player2Id)

            game.castSpell(1, "Erode", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            withClue("the victim can't search, so there is no may-search to offer them") {
                game.hasPendingDecision() shouldBe false
            }
            game.searchForForest(game.player2Id)
            withClue("no basic left the victim's library") {
                game.forestsControlledBy(game.player2Id) shouldBe 0
                game.state.getLibrary(game.player2Id).size shouldBe 2
            }
        }

        test("only the caster can't search libraries: the victim's search of their own library proceeds") {
            val game = erodeGame()
            game.forbidSearching(game.player1Id)

            game.castSpell(1, "Erode", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            withClue("the victim is offered the search — the caster's restriction is not theirs") {
                game.hasPendingDecision() shouldBe true
            }
            game.searchForForest(game.player2Id)
            withClue("the victim fetched a Forest") {
                game.forestsControlledBy(game.player2Id) shouldBe 1
            }
        }

        test("the caster's Wan Shi Tong sees the victim's search as an opponent's search") {
            val game = erodeGame({ it.withCardOnBattlefield(1, "Wan Shi Tong, Librarian").withCardInLibrary(1, "Island") })
            game.castSpell(1, "Erode", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            val handBefore = game.handSize(1)
            game.resolveStack()
            game.searchForForest(game.player2Id)

            withClue("an opponent searched their library: +1/+1 counter and a card for the caster") {
                game.plusOneCounters("Wan Shi Tong, Librarian") shouldBe 1
                game.handSize(1) shouldBe handBefore + 1
            }
        }

        test("Restorative Technique: the target player's search is attributed to that player") {
            // Player 2's Wan Shi Tong must not trigger: player 2 searched their own library, they
            // are not an opponent of themselves. Player 1's does: player 2 is their opponent.
            val game = scenario()
                .withPlayers("Caster", "Target")
                .withCardInHand(1, "Restorative Technique")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardOnBattlefield(1, "Wan Shi Tong, Librarian")
                .withCardInLibrary(1, "Island")
                .withCardOnBattlefield(2, "Wan Shi Tong, Librarian")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val (casterWan, targetWan) = game.state.getBattlefield().filter { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Wan Shi Tong, Librarian"
            }.sortedBy { if (game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player1Id) 0 else 1 }
            val technique = game.state.getHand(game.player1Id).first { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Restorative Technique"
            }

            game.execute(CastSpell(game.player1Id, technique, listOf(ChosenTarget.Player(game.player2Id))))
                .error shouldBe null
            game.resolveStack()
            game.searchForForest(game.player2Id)

            fun counters(id: EntityId) =
                game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
            withClue("the target player fetched the Forest") {
                game.forestsControlledBy(game.player2Id) shouldBe 1
            }
            withClue("the caster's Wan Shi Tong saw an opponent search") {
                counters(casterWan) shouldBe 1
            }
            withClue("the target's own Wan Shi Tong did not — that player searched their own library") {
                counters(targetWan) shouldBe 0
            }
        }
    }
}
