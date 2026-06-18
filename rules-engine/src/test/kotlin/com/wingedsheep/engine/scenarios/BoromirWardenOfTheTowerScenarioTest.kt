package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Boromir, Warden of the Tower — "Whenever an opponent casts a spell, if no mana was spent to cast
 * it, counter that spell." Exercises the new `Conditions.TriggeringSpellCastWithoutPayingMana`
 * intervening-if (reads the triggering spell's cast-mana record).
 */
class BoromirWardenOfTheTowerScenarioTest : FunSpec({

    fun advanceToOpponentMain(driver: GameTestDriver, opponent: EntityId) {
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        driver.activePlayer shouldBe opponent
    }

    test("counters an opponent's spell cast for free (no mana spent)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.putCreatureOnBattlefield(p1, "Boromir, Warden of the Tower")
        advanceToOpponentMain(driver, p2)

        val mox = driver.putCardInHand(p2, "Mox Ruby") // {0} artifact → no mana spent
        driver.castSpell(p2, mox)
        driver.bothPass() // Boromir's trigger resolves and counters the free spell

        driver.state.getGraveyard(p2).contains(mox) shouldBe true
        driver.findPermanent(p2, "Mox Ruby") shouldBe null
    }

    test("does not counter an opponent's spell that mana was spent on") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.putCreatureOnBattlefield(p1, "Boromir, Warden of the Tower")
        advanceToOpponentMain(driver, p2)

        val bears = driver.putCardInHand(p2, "Grizzly Bears") // {1}{G}, paid with mana
        driver.giveMana(p2, Color.GREEN, 2)
        driver.castSpell(p2, bears)
        driver.bothPass()

        // Boromir's intervening-if is false (mana was spent), so the spell resolves.
        driver.findPermanent(p2, "Grizzly Bears").shouldNotBeNull()
        driver.state.getGraveyard(p2).contains(bears) shouldBe false
    }
})
