package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.TowerOfCoireall
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Tower of Coireall — "{T}: Target creature can't be blocked by Walls this turn."
 *
 * The grant is the interesting part: `CantBeBlockedBy` is normally a *printed* static (Bog Rats),
 * and the blocker check has to read the granted copy keyed to the attacker as well. So the test
 * declares the illegal Wall block and expects the engine to reject it, then shows a non-Wall of the
 * same board still blocks — otherwise a grant that accidentally blanked all blocking would pass too.
 */
class TowerOfCoireallScenarioTest : FunSpec({

    val abilityId = TowerOfCoireall.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TowerOfCoireall)
        return driver
    }

    test("a Wall can't block the targeted creature, but a non-Wall still can") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tower = driver.putPermanentOnBattlefield(me, "Tower of Coireall")
        val attacker = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        driver.removeSummoningSickness(attacker)
        val wall = driver.putCreatureOnBattlefield(opponent, "Wall of Granite")
        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = tower,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, attacker)),
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), opponent).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        withClue("the Wall's block is illegal") {
            driver.declareBlockers(opponent, mapOf(wall to listOf(attacker))).isSuccess shouldBe false
        }
        withClue("a non-Wall on the same board still blocks fine") {
            driver.declareBlockers(opponent, mapOf(bear to listOf(attacker))).isSuccess shouldBe true
        }
    }

    test("without the Tower's grant the same Wall blocks") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        driver.removeSummoningSickness(attacker)
        val wall = driver.putCreatureOnBattlefield(opponent, "Wall of Granite")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), opponent).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        withClue("control: the restriction comes from the Tower, not from Walls generally") {
            driver.declareBlockers(opponent, mapOf(wall to listOf(attacker))).isSuccess shouldBe true
        }
    }
})
