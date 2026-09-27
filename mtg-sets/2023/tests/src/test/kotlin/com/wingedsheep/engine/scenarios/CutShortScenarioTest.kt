package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Cut Short — "Destroy target planeswalker that was activated this turn or tapped creature."
 */
class CutShortScenarioTest : ScenarioTestBase() {

    private fun board(vararg opponentPermanents: Pair<String, Boolean>) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Cut Short")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withCardOnBattlefield(1, "Ajani Goldmane")
        .apply { opponentPermanents.forEach { (name, tapped) -> withCardOnBattlefield(2, name, tapped = tapped) } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private val ajaniPlusOne get() = cardRegistry.getCard("Ajani Goldmane")!!.script.activatedAbilities[0].id

    init {
        test("destroys a planeswalker whose loyalty ability was activated and resolved this turn") {
            val game = board()
            val ajani = game.findPermanent("Ajani Goldmane")!!
            game.execute(ActivateAbility(game.player1Id, ajani, ajaniPlusOne)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 22

            game.castSpell(1, "Cut Short", ajani).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Ajani Goldmane") shouldBe true
        }

        test("the activated ability may still be on the stack") {
            val game = board()
            val ajani = game.findPermanent("Ajani Goldmane")!!
            game.execute(ActivateAbility(game.player1Id, ajani, ajaniPlusOne)).error shouldBe null

            game.castSpell(1, "Cut Short", ajani).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Ajani Goldmane") shouldBe true
        }

        test("can't target a planeswalker that wasn't activated this turn") {
            val game = board()
            val ajani = game.findPermanent("Ajani Goldmane")!!
            game.castSpell(1, "Cut Short", ajani).error shouldNotBe null
            game.isOnBattlefield("Ajani Goldmane") shouldBe true
        }

        test("destroys a tapped creature") {
            val game = board("Grizzly Bears" to true)
            game.castSpell(1, "Cut Short", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("can't target an untapped creature") {
            val game = board("Grizzly Bears" to false)
            game.castSpell(1, "Cut Short", game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
