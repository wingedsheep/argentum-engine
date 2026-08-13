package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gpt.cards.Repeal
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Repeal: {X}{U} — return target nonland permanent with mana value X to its owner's hand, then
 * draw a card. Exercises CardPredicate.ManaValueEqualsX target restriction with X bound from the
 * spell's {X}{U} mana cost.
 */
class RepealScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(Repeal)
        driver.initMirrorMatch(deck = Deck.of("Island" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("Repeal with X=2 returns a mana value 2 permanent and draws a card") {
        val driver = setup()
        val player = driver.activePlayer!!

        // Forest Walker is {1}{G} → mana value 2.
        val target = driver.putCreatureOnBattlefield(player, "Forest Walker")
        val repeal = driver.putCardInHand(player, "Repeal")
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveColorlessMana(player, 2) // X = 2

        val result = driver.castXSpell(player, repeal, xValue = 2, targets = listOf(target))
        result.isSuccess shouldBe true
        driver.bothPass()

        // Forest Walker bounced back to its owner's hand.
        driver.findPermanent(player, "Forest Walker") shouldBe null
        driver.findCardInHand(player, "Forest Walker") shouldNotBe null
    }

    test("the enumerated cast advertises that X constrains target mana value exactly") {
        val driver = setup()
        val player = driver.activePlayer!!

        driver.putCreatureOnBattlefield(player, "Forest Walker")
        driver.putCardInHand(player, "Repeal")
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveColorlessMana(player, 2)

        val cast = driver.legalActions(player).find { it.description == "Cast Repeal" }
        cast shouldNotBe null
        // X is unbound at enumeration, so validTargets is permissive and whoever binds X owes the
        // narrowing — the client in its X-selection phase, XCostSelection for the AI. Neither can
        // do it without this flag: "mana value X" is an equality, not the "or less" the plain
        // xConstrainsTargetManaValue means.
        cast!!.xConstrainsTargetManaValueExactly shouldBe true
        cast.xConstrainsTargetManaValue shouldBe false
    }

    test("Repeal with X=2 cannot target a mana value 3 permanent") {
        val driver = setup()
        val player = driver.activePlayer!!

        // Phantom Warrior is {1}{U}{U} → mana value 3.
        val tooBig = driver.putCreatureOnBattlefield(player, "Phantom Warrior")
        val repeal = driver.putCardInHand(player, "Repeal")
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveColorlessMana(player, 2) // X = 2

        val result = driver.castXSpell(player, repeal, xValue = 2, targets = listOf(tooBig))
        result.isSuccess shouldBe false
        driver.findPermanent(player, "Phantom Warrior") shouldBe tooBig
    }
})
