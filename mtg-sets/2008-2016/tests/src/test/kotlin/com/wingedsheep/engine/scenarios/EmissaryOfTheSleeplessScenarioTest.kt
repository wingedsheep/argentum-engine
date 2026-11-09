package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Emissary of the Sleepless (SOI #17) — {4}{W} 2/4 Spirit with flying.
 *
 *   When this creature enters, if a creature died this turn, create a 1/1 white Spirit creature
 *   token with flying.
 */
class EmissaryOfTheSleeplessScenarioTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(driver, player, driver.getOpponent(player))
    }

    fun castEmissary(driver: GameTestDriver, player: EntityId) {
        val emissary = driver.putCardInHand(player, "Emissary of the Sleepless")
        driver.giveMana(player, Color.WHITE, 5)
        driver.castSpell(player, emissary).error shouldBe null
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
        driver.findPermanent(player, "Emissary of the Sleepless").shouldNotBeNull()
    }

    test("if a creature died this turn, it makes a 1/1 white Spirit with flying") {
        val (driver, player, opponent) = newGame()
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 1)
        driver.castSpell(player, bolt, targets = listOf(bears)).error shouldBe null
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
        driver.findPermanent(opponent, "Grizzly Bears").shouldBeNull()

        castEmissary(driver, player)

        val spirit = driver.findPermanent(player, "Spirit Token")
        spirit.shouldNotBeNull()
        driver.state.projectedState.getPower(spirit) shouldBe 1
        driver.state.projectedState.getToughness(spirit) shouldBe 1
        driver.state.projectedState.hasKeyword(spirit, Keyword.FLYING) shouldBe true
    }

    test("with no creature dead this turn, no token is made") {
        val (driver, player, _) = newGame()

        castEmissary(driver, player)

        driver.findPermanent(player, "Spirit Token").shouldBeNull()
    }
})
