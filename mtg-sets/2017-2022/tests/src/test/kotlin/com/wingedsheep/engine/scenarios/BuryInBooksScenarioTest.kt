package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bury in Books — {4}{U} Instant.
 * "This spell costs {2} less to cast if it targets an attacking creature.
 * Put target creature into its owner's library second from the top."
 *
 * Proves the {2} discount applies only when the announced target is attacking, and that the
 * creature lands second from the top of its owner's library.
 */
class BuryInBooksScenarioTest : ScenarioTestBase() {

    init {
        fun combatBoard(islands: Int) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Bury in Books")
            .withLandsOnBattlefield(1, "Island", islands)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Savannah Lions")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Mountain")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
            .also { game ->
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passPriority() // active player passes; defender gets priority
            }

        test("targeting an attacking creature costs {2}{U}; it goes second from the top") {
            val game = combatBoard(islands = 3)
            val bears = game.findPermanent("Grizzly Bears")!!

            withClue("three Islands only pay for the discounted cost") {
                game.castSpell(1, "Bury in Books", bears).error shouldBe null
            }
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
            val library = game.state.getLibrary(game.player2Id)
            withClue("of four cards, index 1 is second from the top and not second from the bottom") {
                library.size shouldBe 4
                library[1] shouldBe bears
            }
        }

        test("a non-attacking creature target gets no discount") {
            val game = combatBoard(islands = 3)
            val lions = game.findPermanent("Savannah Lions")!!

            game.castSpell(1, "Bury in Books", lions).error shouldNotBe null
            game.findPermanent("Savannah Lions") shouldBe lions
        }

        test("full {4}{U} pays for a non-attacking target, which also goes second from the top") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Bury in Books")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Bury in Books", bears).error shouldBe null
            game.resolveStack()

            game.findPermanent("Grizzly Bears") shouldBe null
            game.state.getLibrary(game.player2Id)[1] shouldBe bears
        }
    }
}
