package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ths.cards.TimeToFeed
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Time to Feed (THS #181 — {2}{G} Sorcery)
 * "Choose target creature an opponent controls. When that creature dies this turn, you gain
 * 3 life. Target creature you control fights that creature."
 */
class TimeToFeedScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TimeToFeed)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.damageOn(id: EntityId): Int =
        state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    fun GameTestDriver.resolveStack() {
        while (state.stack.isNotEmpty()) bothPass()
    }

    test("the opposing creature dying to the fight gains you 3 life") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3
        val lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions") // 1/1
        val spell = driver.putCardInHand(me, "Time to Feed")
        driver.giveMana(me, Color.GREEN, 3)
        driver.castSpell(me, spell, listOf(lions, courser))
        driver.resolveStack()

        driver.findPermanent(opponent, "Savannah Lions") shouldBe null
        driver.damageOn(courser) shouldBe 1
        driver.getLifeTotal(me) shouldBe 23
    }

    test("a survivor that dies later this turn still gains you 3 life") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val lions = driver.putCreatureOnBattlefield(me, "Savannah Lions") // 1/1
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3
        val spell = driver.putCardInHand(me, "Time to Feed")
        driver.giveMana(me, Color.GREEN, 3)
        driver.castSpell(me, spell, listOf(courser, lions))
        driver.resolveStack()

        driver.findPermanent(me, "Savannah Lions") shouldBe null
        driver.findPermanent(opponent, "Centaur Courser") shouldNotBe null
        driver.getLifeTotal(me) shouldBe 20

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(courser))
        driver.resolveStack()

        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
        driver.getLifeTotal(me) shouldBe 23
    }

    test("if your creature is gone there is no fight, but the trigger still watches theirs") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser") // 3/3
        val lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions") // 1/1
        val spell = driver.putCardInHand(me, "Time to Feed")
        driver.giveMana(me, Color.GREEN, 3)
        driver.castSpell(me, spell, listOf(lions, courser))
        driver.moveToGraveyard(courser)
        driver.resolveStack()

        driver.findPermanent(opponent, "Savannah Lions") shouldNotBe null
        driver.damageOn(lions) shouldBe 0
        driver.getLifeTotal(me) shouldBe 20

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(lions))
        driver.resolveStack()

        driver.findPermanent(opponent, "Savannah Lions") shouldBe null
        driver.getLifeTotal(me) shouldBe 23
    }
})
