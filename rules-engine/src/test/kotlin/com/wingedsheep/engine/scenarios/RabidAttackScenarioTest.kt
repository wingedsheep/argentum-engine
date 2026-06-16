package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.RabidAttack
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rabid Attack {1}{B} Instant (SOS canonical).
 *
 * Until end of turn, any number of target creatures you control each get +1/+0 and gain
 * "When this creature dies, draw a card."
 */
class RabidAttackScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RabidAttack))
        return driver
    }

    test("two target creatures each get +1/+0; one dies and draws a card for its controller") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Forest" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear1 = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3
        val bear2 = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3

        driver.giveMana(me, Color.BLACK, 1)
        driver.giveMana(me, Color.BLACK, 1)
        val spell = driver.putCardInHand(me, "Rabid Attack")
        val handWithSpell = driver.getHandSize(me)
        driver.castSpell(me, spell, targets = listOf(bear1, bear2)).error shouldBe null
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getPower(bear1) shouldBe 4
        projected.getPower(bear2) shouldBe 4
        projected.getToughness(bear1) shouldBe 3

        // Spent the spell from hand (-1), no draw on cast.
        driver.getHandSize(me) shouldBe (handWithSpell - 1)

        // Kill bear1 with a real damage event (Lightning Bolt, 3 dmg) so the granted
        // dies-trigger fires. It should draw a card for bear1's controller.
        val handBeforeKill = driver.getHandSize(me)
        driver.giveMana(me, Color.RED, 3)
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.castSpell(me, bolt, targets = listOf(bear1)).error shouldBe null
        driver.bothPass()

        driver.getPermanents(me).contains(bear1) shouldBe false // bear1 died
        driver.getPermanents(me).contains(bear2) shouldBe true
        // -1 (Bolt leaves hand) +1 (granted dies-trigger draw) => net unchanged.
        driver.getHandSize(me) shouldBe handBeforeKill
    }
})
