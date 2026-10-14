package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nick Valentine, Private Eye (PIP #35) — {2}{U} Legendary Artifact Creature — Synth Detective 2/2.
 *
 *   Nick Valentine can't be blocked except by artifact creatures.
 *   Whenever Nick Valentine or another artifact creature you control dies, you may investigate.
 */
class NickValentinePrivateEyeScenarioTest : ScenarioTestBase() {

    private val nick = "Nick Valentine, Private Eye"

    private fun TestGame.clues(): Int = findPermanents("Clue").size

    private fun combat(blocker: String): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, nick)
            .withCardOnBattlefield(2, blocker)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf(nick to 2)).error shouldBe null
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        return game
    }

    private fun boltBoard(vararg mine: String): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, nick)
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        mine.forEach { builder = builder.withCardOnBattlefield(1, it) }
        return builder.build()
    }

    init {
        test("a non-artifact creature can't block Nick") {
            val game = combat("Grizzly Bears")
            game.declareBlockers(mapOf("Grizzly Bears" to listOf(nick))).error shouldNotBe null
        }

        test("an artifact creature can block Nick") {
            val game = combat("Ornithopter")
            game.declareBlockers(mapOf("Ornithopter" to listOf(nick))).error shouldBe null
        }

        test("when another artifact creature you control dies, you may investigate") {
            val game = boltBoard("Ornithopter")
            val ornithopter = game.findPermanent("Ornithopter")!!

            game.castSpell(1, "Lightning Bolt", ornithopter).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Ornithopter") shouldBe true
            withClue("the may-investigate prompt is up") { game.hasPendingDecision() shouldBe true }

            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.clues() shouldBe 1
        }

        test("declining the investigate makes no Clue") {
            val game = boltBoard("Ornithopter")
            val ornithopter = game.findPermanent("Ornithopter")!!

            game.castSpell(1, "Lightning Bolt", ornithopter).error shouldBe null
            game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()
            game.clues() shouldBe 0
        }

        test("a non-artifact creature you control dying doesn't trigger") {
            val game = boltBoard("Grizzly Bears")
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.hasPendingDecision() shouldBe false
            game.clues() shouldBe 0
        }

        test("Nick dying triggers his own investigate") {
            val game = boltBoard()
            val nickId = game.findPermanent(nick)!!

            game.castSpell(1, "Lightning Bolt", nickId).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, nick) shouldBe true
            withClue("the may-investigate prompt is up") { game.hasPendingDecision() shouldBe true }

            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.clues() shouldBe 1
        }
    }
}
