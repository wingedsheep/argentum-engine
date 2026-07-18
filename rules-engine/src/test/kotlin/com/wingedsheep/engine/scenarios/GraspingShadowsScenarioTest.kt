package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.GraspingShadows
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Grasping Shadows // Shadows' Lair (LCI #108).
 *
 * Front — Grasping Shadows: "Whenever a creature you control attacks alone, it gains deathtouch
 * and lifelink until end of turn. Put a dread counter on this enchantment. Then if there are
 * three or more dread counters on it, transform it."
 *
 * Pins the attacks-alone grant + dread accrual and the third-counter flip into Shadows' Lair.
 */
class GraspingShadowsScenarioTest : FunSpec({

    val projector = StateProjector()

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GraspingShadows))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        return driver
    }

    fun cardName(driver: GameTestDriver, id: EntityId): String? =
        driver.state.getEntity(id)?.get<CardComponent>()?.name

    fun dreadCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.DREAD) ?: 0

    fun attackAlone(driver: GameTestDriver, attacker: EntityId, p1: EntityId, p2: EntityId) {
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(attacker), p2).error shouldBe null
        var guard = 0
        while (guard++ < 30 && (driver.pendingDecision != null || driver.state.stack.isNotEmpty())) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    test("attacking alone grants deathtouch + lifelink and adds a dread counter") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        val shadows = driver.putPermanentOnBattlefield(p1, "Grasping Shadows")
        val lion = driver.putCreatureOnBattlefield(p1, "Savannah Lions")
        driver.removeSummoningSickness(lion)

        attackAlone(driver, lion, p1, p2)

        val projected = projector.project(driver.state)
        projected.hasKeyword(lion, Keyword.DEATHTOUCH) shouldBe true
        projected.hasKeyword(lion, Keyword.LIFELINK) shouldBe true
        dreadCounters(driver, shadows) shouldBe 1
        cardName(driver, shadows) shouldBe "Grasping Shadows"
    }

    test("the third dread counter transforms Grasping Shadows into Shadows' Lair") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        val shadows = driver.putPermanentOnBattlefield(p1, "Grasping Shadows")
        // Seed two dread counters so this lone attack reaches the three-counter threshold.
        driver.addComponent(shadows, CountersComponent(mapOf(CounterType.DREAD to 2)))
        val lion = driver.putCreatureOnBattlefield(p1, "Savannah Lions")
        driver.removeSummoningSickness(lion)

        attackAlone(driver, lion, p1, p2)

        cardName(driver, shadows) shouldBe "Shadows' Lair"
    }
})
