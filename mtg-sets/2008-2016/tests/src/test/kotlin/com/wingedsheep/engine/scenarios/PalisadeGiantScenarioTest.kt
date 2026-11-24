package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Palisade Giant: damage that would be dealt to its controller or to another permanent they
 * control is dealt to the Giant instead; damage aimed at the Giant itself lands on it normally.
 */
class PalisadeGiantScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30, "Mountain" to 10), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.damageOn(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<DamageComponent>()?.amount ?: 0

    fun GameTestDriver.bolt(caster: EntityId, target: ChosenTarget) {
        giveMana(caster, Color.RED, 1)
        val bolt = putCardInHand(caster, "Lightning Bolt")
        castSpellWithTargets(caster, bolt, listOf(target))
        bothPass()
    }

    test("damage to you is dealt to Palisade Giant instead") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val giant = driver.putCreatureOnBattlefield(p1, "Palisade Giant")

        driver.bolt(p1, ChosenTarget.Player(p1))

        driver.getLifeTotal(p1) shouldBe 20
        driver.damageOn(giant) shouldBe 3
    }

    test("damage to another permanent you control is dealt to Palisade Giant instead") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val giant = driver.putCreatureOnBattlefield(p1, "Palisade Giant")
        val courser = driver.putCreatureOnBattlefield(p1, "Centaur Courser")

        driver.bolt(p1, ChosenTarget.Permanent(courser))

        driver.damageOn(courser) shouldBe 0
        driver.damageOn(giant) shouldBe 3
    }

    test("damage to Palisade Giant itself is not redirected, and opponents' permanents are unaffected") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        val giant = driver.putCreatureOnBattlefield(p1, "Palisade Giant")
        val enemyCourser = driver.putCreatureOnBattlefield(p2, "Centaur Courser")

        driver.bolt(p1, ChosenTarget.Permanent(giant))
        driver.damageOn(giant) shouldBe 3

        driver.bolt(p1, ChosenTarget.Permanent(enemyCourser))
        driver.state.getBattlefield().contains(enemyCourser) shouldBe false // lethal 3 on a 3/3
        driver.damageOn(giant) shouldBe 3

        driver.bolt(p1, ChosenTarget.Player(p2))
        driver.getLifeTotal(p2) shouldBe 17
    }
})
