package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.IanTheReckless
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ian the Reckless {1}{R} — Legendary Creature — Human Warrior 2/1
 *   Whenever Ian the Reckless attacks, if it's modified, you may have it deal damage equal to its
 *   power to you and any target.
 *
 * Proves the intervening-if gates the trigger, the optional damage hits both you and the target,
 * and (ruling 2024-03-08) a modified Ian killed in response still resolves on last-known
 * information: it was modified, and its last power sets the damage.
 */
class IanTheRecklessScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(IanTheReckless))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun ianWithCounter(driver: GameTestDriver, me: EntityId): EntityId {
        val ian = driver.putCreatureOnBattlefield(me, "Ian the Reckless")
        driver.removeSummoningSickness(ian)
        driver.replaceState(
            driver.state.updateEntity(ian) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }
        )
        return ian
    }

    // The lone target prompt also answers the "you may" (choosing a target is yes, none is no),
    // so no separate yes/no follows on resolution.
    fun attackAndTarget(driver: GameTestDriver, me: EntityId, ian: EntityId, targets: List<EntityId>) {
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(ian), defendingPlayer = driver.getOpponent(me)).error shouldBe null
        driver.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, targets).error shouldBe null
    }

    test("modified Ian deals its power to you and the target you choose") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val ian = ianWithCounter(driver, me)

        attackAndTarget(driver, me, ian, listOf(opponent))
        driver.bothPass()

        driver.getLifeTotal(me) shouldBe 17
        driver.getLifeTotal(opponent) shouldBe 17
    }

    test("choosing no target declines: no damage") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val ian = ianWithCounter(driver, me)

        attackAndTarget(driver, me, ian, emptyList())
        driver.stackSize shouldBe 0

        driver.getLifeTotal(me) shouldBe 20
        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("an unmodified Ian doesn't trigger") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val ian = driver.putCreatureOnBattlefield(me, "Ian the Reckless")
        driver.removeSummoningSickness(ian)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(ian), defendingPlayer = opponent).error shouldBe null

        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe false
        driver.stackSize shouldBe 0
    }

    test("killed in response, Ian still deals its last-known power on last-known modification") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val ian = ianWithCounter(driver, me)

        attackAndTarget(driver, me, ian, listOf(opponent))

        // Bolt Ian (a 3/2 with its counter) in response to its trigger.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(ian)).error shouldBe null
        driver.bothPass()
        driver.getGraveyard(me).contains(ian) shouldBe true

        driver.bothPass()

        driver.getLifeTotal(me) shouldBe 17
        driver.getLifeTotal(opponent) shouldBe 17
    }
})
