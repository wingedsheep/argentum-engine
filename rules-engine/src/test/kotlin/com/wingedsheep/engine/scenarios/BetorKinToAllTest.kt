package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** A 0/20 vanilla wall: a couple of these let the battlefield clear Betor's 20- and 40-toughness gates. */
private val ToughnessWall = CardDefinition.creature(
    name = "Toughness Wall",
    manaCost = ManaCost.parse("{4}{G}{G}"),
    subtypes = setOf(Subtype("Wall")),
    power = 0,
    toughness = 20
)

/**
 * Tests for Betor, Kin to All ({2}{W}{B}{G}, 5/7, Flying):
 * "At the beginning of your end step, if creatures you control have total toughness 10 or
 *  greater, draw a card. Then if ... 20 or greater, untap each creature you control. Then if
 *  ... 40 or greater, each opponent loses half their life, rounded up."
 *
 * The toughness thresholds use `Compare(AggregateBattlefield(SUM, TOUGHNESS), GTE, n)` over
 * projected toughness; the 10 gate is the trigger's intervening "if".
 */
class BetorKinToAllTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ToughnessWall)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun advanceToEndStep(driver: GameTestDriver, targetPlayer: EntityId) {
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.activePlayer shouldBe targetPlayer
        driver.currentStep shouldBe Step.END
    }

    test("total toughness 10+: the end-step trigger draws a card") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        // Betor (toughness 7) + Hill Giant (toughness 3) = 10 total toughness — clears the draw gate.
        driver.putCreatureOnBattlefield(me, "Betor, Kin to All")
        driver.putCreatureOnBattlefield(me, "Hill Giant")

        val handBefore = driver.state.getZone(ZoneKey(me, Zone.HAND)).size

        advanceToEndStep(driver, me)
        driver.bothPass() // resolve the end-step trigger

        driver.state.getZone(ZoneKey(me, Zone.HAND)).size shouldBe handBefore + 1
    }

    test("total toughness below 10: the intervening if fails, no draw") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        // Betor alone = 7 toughness, under the 10 threshold.
        driver.putCreatureOnBattlefield(me, "Betor, Kin to All")

        val handBefore = driver.state.getZone(ZoneKey(me, Zone.HAND)).size

        advanceToEndStep(driver, me)
        driver.bothPass()

        driver.state.getZone(ZoneKey(me, Zone.HAND)).size shouldBe handBefore
    }

    test("total toughness 20+: each creature you control untaps; the 40 gate stays closed") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Betor (7) + Toughness Wall (20) = 27 — clears the draw (10) and untap (20) gates, not 40.
        val betor = driver.putCreatureOnBattlefield(me, "Betor, Kin to All")
        val wall = driver.putCreatureOnBattlefield(me, "Toughness Wall")
        driver.tapPermanent(betor)
        driver.tapPermanent(wall)

        val oppLifeBefore = driver.getLifeTotal(opp)

        advanceToEndStep(driver, me)
        driver.bothPass() // resolve the end-step trigger

        driver.isTapped(betor) shouldBe false
        driver.isTapped(wall) shouldBe false
        // 40-toughness gate not cleared: each opponent's life is untouched.
        driver.getLifeTotal(opp) shouldBe oppLifeBefore
    }

    test("total toughness 40+: each opponent loses half their life, rounded up") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Betor (7) + two Toughness Walls (20 each) = 47 — clears all three gates.
        driver.putCreatureOnBattlefield(me, "Betor, Kin to All")
        driver.putCreatureOnBattlefield(me, "Toughness Wall")
        driver.putCreatureOnBattlefield(me, "Toughness Wall")

        // 21 life makes the round-up observable: ceil(21 / 2) = 11 lost -> 10 remaining.
        driver.setLifeTotal(opp, 21)

        advanceToEndStep(driver, me)
        driver.bothPass()

        driver.getLifeTotal(opp) shouldBe 10
    }
})
