package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Synchronized Eviction — {4}{U} Instant.
 * "This spell costs {2} less to cast if you control at least two creatures that share a creature type.
 * Put target nonland permanent into its owner's library second from the top."
 *
 * Proves the {2} discount is gated on two of *your* creatures sharing a creature type (changelings
 * count), that the full cost still pays without it, that any nonland permanent (not a land) can be
 * targeted, and that the permanent lands second from the top of its owner's library.
 */
class SynchronizedEvictionScenarioTest : ScenarioTestBase() {

    init {
        fun board(
            islands: Int,
            yourCreatures: List<String>,
            theirCreatures: List<String> = emptyList(),
        ) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Synchronized Eviction")
            .withLandsOnBattlefield(1, "Island", islands)
            .apply { yourCreatures.forEach { withCardOnBattlefield(1, it) } }
            .apply { theirCreatures.forEach { withCardOnBattlefield(2, it) } }
            .withCardOnBattlefield(2, "Icy Manipulator")
            .withCardOnBattlefield(2, "Forest")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Mountain")
            .withCardInLibrary(2, "Plains")
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("two creatures sharing a type discount it to {2}{U}; the target goes second from the top") {
            val game = board(islands = 3, yourCreatures = listOf("Grizzly Bears", "Grizzly Bears"))
            val icy = game.findPermanent("Icy Manipulator")!!

            withClue("three Islands only pay for the discounted cost") {
                game.castSpell(1, "Synchronized Eviction", icy).error shouldBe null
            }
            game.resolveStack()

            game.findPermanent("Icy Manipulator") shouldBe null
            val library = game.state.getLibrary(game.player2Id)
            withClue("of four cards, index 1 is second from the top of the owner's library") {
                library.size shouldBe 4
                library[1] shouldBe icy
            }
        }

        test("two creatures with no common type get no discount") {
            val game = board(islands = 3, yourCreatures = listOf("Grizzly Bears", "Savannah Lions"))
            val icy = game.findPermanent("Icy Manipulator")!!

            game.castSpell(1, "Synchronized Eviction", icy).error shouldNotBe null
            game.findPermanent("Icy Manipulator") shouldBe icy
        }

        test("an opponent's tribe doesn't count toward the discount") {
            val game = board(
                islands = 3,
                yourCreatures = listOf("Grizzly Bears"),
                theirCreatures = listOf("Grizzly Bears", "Grizzly Bears"),
            )
            val icy = game.findPermanent("Icy Manipulator")!!

            game.castSpell(1, "Synchronized Eviction", icy).error shouldNotBe null
        }

        test("a changeling shares a creature type with any other creature") {
            val game = board(islands = 3, yourCreatures = listOf("Changeling Berserker", "Savannah Lions"))
            val icy = game.findPermanent("Icy Manipulator")!!

            game.castSpell(1, "Synchronized Eviction", icy).error shouldBe null
            game.resolveStack()
            game.state.getLibrary(game.player2Id)[1] shouldBe icy
        }

        test("full {4}{U} pays without the discount and can evict a creature") {
            val game = board(islands = 5, yourCreatures = emptyList(), theirCreatures = listOf("Savannah Lions"))
            val lions = game.findPermanent("Savannah Lions")!!

            game.castSpell(1, "Synchronized Eviction", lions).error shouldBe null
            game.resolveStack()

            game.findPermanent("Savannah Lions") shouldBe null
            game.state.getLibrary(game.player2Id)[1] shouldBe lions
        }

        test("a land is not a legal target") {
            val game = board(islands = 5, yourCreatures = emptyList())
            val forest = game.findPermanent("Forest")!!

            game.castSpell(1, "Synchronized Eviction", forest).error shouldNotBe null
            game.findPermanent("Icy Manipulator") shouldNotBe null
        }
    }
}
