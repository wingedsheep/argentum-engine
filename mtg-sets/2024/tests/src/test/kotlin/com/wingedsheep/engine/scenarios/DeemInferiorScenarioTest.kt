package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Deem Inferior — {3}{U} Sorcery.
 * "This spell costs {1} less to cast for each card you've drawn this turn. The owner of target
 * nonland permanent puts it into their library second from the top or on the bottom."
 *
 * Proves the self-cast dynamic reduction reads the *caster's* draw count, and that the target's
 * owner (not the caster) picks the library position.
 */
class DeemInferiorScenarioTest : ScenarioTestBase() {

    init {
        fun board(drawn: Int, islands: Int) = scenario()
            .withPlayers()
            .withCardInHand(1, "Deem Inferior")
            .withLandsOnBattlefield(1, "Island", islands)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardsDrawnThisTurn(1, drawn)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Mountain")
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("two cards drawn this turn — castable for {1}{U}; owner puts it second from the top") {
            val game = board(drawn = 2, islands = 2)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Deem Inferior", bears).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as ChooseOptionDecision
            decision.playerId shouldBe game.player2Id
            game.submitDecision(OptionChosenResponse(decision.id, 0)) // second from top

            game.findPermanent("Grizzly Bears") shouldBe null
            val library = game.state.getLibrary(game.player2Id)
            library.size shouldBe 3
            library[1] shouldBe bears
        }

        test("owner may choose the bottom instead") {
            val game = board(drawn = 2, islands = 2)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Deem Inferior", bears).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as ChooseOptionDecision
            game.submitDecision(OptionChosenResponse(decision.id, 1)) // bottom

            game.state.getLibrary(game.player2Id).last() shouldBe bears
        }

        test("no cards drawn — two Islands can't pay the full {3}{U}") {
            val game = board(drawn = 0, islands = 2)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Deem Inferior", bears).error shouldNotBe null
            game.findPermanent("Grizzly Bears") shouldBe bears
        }

        test("one card drawn — {2}{U}: three Islands suffice, two don't") {
            board(drawn = 1, islands = 2).let { game ->
                game.castSpell(1, "Deem Inferior", game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
            }
            board(drawn = 1, islands = 3).let { game ->
                game.castSpell(1, "Deem Inferior", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            }
        }

        test("reduction never goes below the {U} — five draws still need one Island") {
            board(drawn = 5, islands = 1).let { game ->
                game.castSpell(1, "Deem Inferior", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            }
            board(drawn = 5, islands = 0).let { game ->
                game.castSpell(1, "Deem Inferior", game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
            }
        }
    }
}
