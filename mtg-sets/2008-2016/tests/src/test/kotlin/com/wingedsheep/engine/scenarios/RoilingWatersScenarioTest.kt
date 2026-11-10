package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Roiling Waters — {5}{U}{U} Sorcery.
 * "Return up to two target creatures your opponents control to their owners' hands.
 * Target player draws two cards."
 */
class RoilingWatersScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Roiling Waters")
            .withLandsOnBattlefield(1, "Island", 7)
            .withCardOnBattlefield(1, "Serra Angel")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("bounces two opposing creatures and the target player draws two") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val spell = game.findCardsInHand(1, "Roiling Waters").single()

            val result = game.execute(
                CastSpell(
                    game.player1Id, spell,
                    listOf(ChosenTarget.Permanent(giant), ChosenTarget.Permanent(bears), ChosenTarget.Player(game.player1Id)),
                )
            )
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            withClue("both creatures return to their owner's hand") {
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInHand(2, "Hill Giant") shouldBe true
                game.isInHand(2, "Grizzly Bears") shouldBe true
            }
            withClue("the targeted player draws two") {
                game.handSize(1) shouldBe 2
                game.librarySize(1) shouldBe 1
            }
            game.isOnBattlefield("Serra Angel") shouldBe true
        }

        test("up to two: a single creature, or none, still lets the player draw") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            val spell = game.findCardsInHand(1, "Roiling Waters").single()

            val result = game.execute(
                CastSpell(game.player1Id, spell, listOf(ChosenTarget.Permanent(giant), ChosenTarget.Player(game.player1Id)))
            )
            withClue("one creature plus the player is a legal cast: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInHand(2, "Hill Giant") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.handSize(1) shouldBe 2
        }

        test("no creature targets: only the player draws") {
            val game = board()
            val spell = game.findCardsInHand(1, "Roiling Waters").single()

            val result = game.execute(CastSpell(game.player1Id, spell, listOf(ChosenTarget.Player(game.player1Id))))
            withClue("the player alone is a legal cast: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.handSize(1) shouldBe 2
        }

        test("a creature you control can't be targeted") {
            val game = board()
            val angel = game.findPermanent("Serra Angel")!!
            val giant = game.findPermanent("Hill Giant")!!
            val spell = game.findCardsInHand(1, "Roiling Waters").single()

            game.execute(
                CastSpell(
                    game.player1Id, spell,
                    listOf(ChosenTarget.Permanent(angel), ChosenTarget.Permanent(giant), ChosenTarget.Player(game.player1Id)),
                )
            ).error shouldNotBe null
            game.isOnBattlefield("Serra Angel") shouldBe true
        }
    }
}
