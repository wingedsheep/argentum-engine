package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Liliana's Defeat (HOU #68) — {B} Sorcery.
 *
 *   Destroy target black creature or black planeswalker. If that permanent was a Liliana
 *   planeswalker, her controller loses 3 life.
 *
 * The Liliana check is read before the destroy; the life loss goes to the destroyed permanent's
 * (last-known) controller, not the caster.
 */
class LilianasDefeatScenarioTest : ScenarioTestBase() {

    init {
        fun board(victim: String) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Liliana's Defeat")
            .withLandsOnBattlefield(1, "Swamp", 1)
            .withCardOnBattlefield(2, victim)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("destroys a black creature; no life is lost") {
            val game = board("Typhoid Rats")

            game.castSpell(1, "Liliana's Defeat", targetId = game.findPermanent("Typhoid Rats")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Typhoid Rats") shouldBe true
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 20
        }

        test("destroying a Liliana planeswalker makes her controller lose 3 life") {
            val game = board("Liliana of the Veil")

            game.castSpell(1, "Liliana's Defeat", targetId = game.findPermanent("Liliana of the Veil")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Liliana of the Veil") shouldBe true
            withClue("Liliana's controller loses 3") { game.getLifeTotal(2) shouldBe 17 }
            withClue("the caster is untouched") { game.getLifeTotal(1) shouldBe 20 }
        }

        test("a black planeswalker that isn't a Liliana is destroyed without life loss") {
            val game = board("Ob Nixilis, the Hate-Twisted")

            game.castSpell(1, "Liliana's Defeat", targetId = game.findPermanent("Ob Nixilis, the Hate-Twisted")!!)
                .error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Ob Nixilis, the Hate-Twisted") shouldBe true
            game.getLifeTotal(2) shouldBe 20
        }

        test("a nonblack creature can't be targeted") {
            val game = board("Grizzly Bears")

            game.castSpell(1, "Liliana's Defeat", targetId = game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
        }
    }
}
