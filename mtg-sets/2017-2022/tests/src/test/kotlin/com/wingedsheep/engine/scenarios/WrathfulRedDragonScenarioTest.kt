package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.clb.cards.WrathfulRedDragon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Wrathful Red Dragon (CLB #207): "Whenever a Dragon you control is dealt damage, it deals that much
 * damage to any target that isn't a Dragon."
 *
 * The damaged Dragon is the source of the reflected damage, and one the damage killed still deals
 * it (last-known information). Dragons can't be targeted; players and non-Dragons can.
 */
class WrathfulRedDragonScenarioTest : FunSpec({

    val smallDragon = card("Small Dragon") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Dragon"
        power = 2
        toughness = 2
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WrathfulRedDragon, smallDragon))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Bolt [victim]; answer the reflect trigger with [reflectTarget], or capture its decision. */
    fun boltDragon(driver: GameTestDriver, victim: EntityId, reflectTarget: EntityId?): ChooseTargetsDecision? {
        val p1 = driver.player1
        driver.giveMana(p1, Color.RED, 1)
        val bolt = driver.putCardInHand(p1, "Lightning Bolt")
        driver.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Permanent(victim))).error shouldBe null
        var guard = 0
        while (guard++ < 16) {
            when (val decision = driver.state.pendingDecision) {
                is ChooseTargetsDecision -> {
                    if (reflectTarget == null) return decision
                    driver.submitTargetSelection(decision.playerId, listOf(reflectTarget))
                }
                null -> {
                    if (driver.state.stack.isEmpty()) return null
                    driver.bothPass()
                }
                else -> driver.autoResolveDecision()
            }
        }
        return null
    }

    test("damage to the Dragon itself is reflected at an opponent") {
        val driver = newDriver()
        val wrathful = driver.putCreatureOnBattlefield(driver.player1, "Wrathful Red Dragon")

        boltDragon(driver, victim = wrathful, reflectTarget = driver.player2)

        driver.getLifeTotal(driver.player2) shouldBe 17
    }

    test("two Wrathful Red Dragons each reflect the damage") {
        val driver = newDriver()
        driver.putCreatureOnBattlefield(driver.player1, "Wrathful Red Dragon")
        val second = driver.putCreatureOnBattlefield(driver.player1, "Wrathful Red Dragon")

        boltDragon(driver, victim = second, reflectTarget = driver.player2)

        driver.getLifeTotal(driver.player2) shouldBe 14
    }

    test("a Dragon killed by the damage still deals it") {
        val driver = newDriver()
        driver.putCreatureOnBattlefield(driver.player1, "Wrathful Red Dragon")
        val whelp = driver.putCreatureOnBattlefield(driver.player1, "Small Dragon")

        boltDragon(driver, victim = whelp, reflectTarget = driver.player2)

        driver.getGraveyardCardNames(driver.player1) shouldContain "Small Dragon"
        driver.getLifeTotal(driver.player2) shouldBe 17
    }

    test("Dragons can't be targeted; players and other creatures can") {
        val driver = newDriver()
        val wrathful = driver.putCreatureOnBattlefield(driver.player1, "Wrathful Red Dragon")
        val enemyDragon = driver.putCreatureOnBattlefield(driver.player2, "Small Dragon")
        val bears = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")

        val decision = boltDragon(driver, victim = wrathful, reflectTarget = null)
        decision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = decision.legalTargets.getValue(0)
        legal shouldNotContain enemyDragon
        legal shouldNotContain wrathful
        legal shouldContain bears
        legal shouldContain driver.player2
        legal shouldContain driver.player1
    }

    test("damage to an opponent's Dragon doesn't trigger") {
        val driver = newDriver()
        driver.putCreatureOnBattlefield(driver.player1, "Wrathful Red Dragon")
        val enemyDragon = driver.putCreatureOnBattlefield(driver.player2, "Small Dragon")

        boltDragon(driver, victim = enemyDragon, reflectTarget = null) shouldBe null
        driver.getLifeTotal(driver.player2) shouldBe 20
    }
})
