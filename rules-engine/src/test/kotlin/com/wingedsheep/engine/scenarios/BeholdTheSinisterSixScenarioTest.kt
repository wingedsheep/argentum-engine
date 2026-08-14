package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Behold the Sinister Six! (SPM) — "Return up to six target creature cards with different names
 * from your graveyard to the battlefield." Pins the new `TargetObject.differentNames` cross-target
 * constraint (`TargetValidator` / `DecisionValidators`).
 */
class BeholdTheSinisterSixScenarioTest : FunSpec({

    val spiderA = card("Behold Test Spider A") {
        manaCost = "{1}"
        typeLine = "Creature — Spider"
        power = 1
        toughness = 1
    }
    val spiderB = card("Behold Test Spider B") {
        manaCost = "{1}"
        typeLine = "Creature — Spider"
        power = 1
        toughness = 1
    }

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(spiderA, spiderB))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    test("different-named creature cards all return to the battlefield") {
        val (driver, you) = newGame()
        val a = driver.putCardInGraveyard(you, "Behold Test Spider A")
        val b = driver.putCardInGraveyard(you, "Behold Test Spider B")
        driver.giveMana(you, Color.BLACK, 7)
        val behold = driver.putCardInHand(you, "Behold the Sinister Six!")

        val result = driver.castSpellWithTargets(
            you, behold,
            listOf(ChosenTarget.Card(a, you, Zone.GRAVEYARD), ChosenTarget.Card(b, you, Zone.GRAVEYARD))
        )
        result.error shouldBe null
        resolveStack(driver)

        driver.state.getBattlefield().contains(a) shouldBe true
        driver.state.getBattlefield().contains(b) shouldBe true
    }

    test("two same-named creature cards can't both be targeted (different names required)") {
        val (driver, you) = newGame()
        val a1 = driver.putCardInGraveyard(you, "Behold Test Spider A")
        val a2 = driver.putCardInGraveyard(you, "Behold Test Spider A")
        driver.giveMana(you, Color.BLACK, 7)
        val behold = driver.putCardInHand(you, "Behold the Sinister Six!")

        val result = driver.castSpellWithTargets(
            you, behold,
            listOf(ChosenTarget.Card(a1, you, Zone.GRAVEYARD), ChosenTarget.Card(a2, you, Zone.GRAVEYARD))
        )
        result.error shouldNotBe null // rejected: same name
    }
})
