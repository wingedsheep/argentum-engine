package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.MinisterOfPain
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Minister of Pain (DTK #111) — {2}{B} 2/3 Human Shaman.
 *
 * "Exploit. When this creature exploits a creature, creatures your opponents control get -1/-1
 * until end of turn."
 *
 * Pins that the sweep hits only the opponents' creatures (a 1/1 dies, a 3/3 becomes 2/2, the
 * Minister's own side is untouched) and that declining the sacrifice shrinks nothing.
 */
class MinisterOfPainScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MinisterOfPain))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Swamp" to 20, "Grizzly Bears" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castMinister(driver: GameTestDriver, me: EntityId): EntityId {
        val card = driver.putCardInHand(me, "Minister of Pain")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, card).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the creature spell -> exploit trigger goes on the stack
        driver.bothPass() // resolve the exploit trigger -> optional sacrifice decision
        return card
    }

    test("exploiting gives only the opponents' creatures -1/-1") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val fodder = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val myCourser = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val oppLions = driver.putCreatureOnBattlefield(opp, "Savannah Lions")
        val oppCourser = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val minister = castMinister(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.submitCardSelection(me, listOf(fodder))
        driver.bothPass() // resolve the reflexive -1/-1 payoff

        driver.getGraveyard(me) shouldContain fodder
        driver.getGraveyard(opp) shouldContain oppLions
        driver.state.projectedState.getPower(oppCourser) shouldBe 2
        driver.state.projectedState.getToughness(oppCourser) shouldBe 2
        driver.state.projectedState.getPower(myCourser) shouldBe 3
        driver.state.projectedState.getToughness(myCourser) shouldBe 3
        driver.state.projectedState.getPower(minister) shouldBe 2
        driver.state.projectedState.getToughness(minister) shouldBe 3
    }

    test("declining the exploit sacrifice leaves the opponents' creatures alone") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val oppBears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        castMinister(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        driver.getCreatures(opp) shouldContain oppBears
        driver.state.projectedState.getPower(oppBears) shouldBe 2
        driver.state.projectedState.getToughness(oppBears) shouldBe 2
    }
})
