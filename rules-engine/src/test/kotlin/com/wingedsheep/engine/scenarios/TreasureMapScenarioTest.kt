package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.TreasureMap
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Treasure Map // Treasure Cove (LCI #267).
 *
 * Front — Treasure Map: "{1}, {T}: Scry 1. Put a landmark counter on this artifact. Then if
 * there are three or more landmark counters on it, remove those counters, transform this
 * artifact, and create three Treasure tokens."
 *
 * Pins the landmark-counter accrual and the third-counter flip into Treasure Cove + three
 * Treasures, plus the sub-threshold case (one activation just adds a counter).
 */
class TreasureMapScenarioTest : FunSpec({

    val abilityId = TreasureMap.activatedAbilities.single().id

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TreasureMap) + PredefinedTokens.allTokens)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun cardName(driver: GameTestDriver, id: EntityId): String? =
        driver.state.getEntity(id)?.get<CardComponent>()?.name

    fun landmarkCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LANDMARK) ?: 0

    fun activateAndResolve(driver: GameTestDriver, player: EntityId, map: EntityId) {
        driver.giveMana(player, Color.BLUE, 1) // pays the generic {1}
        driver.submitSuccess(ActivateAbility(playerId = player, sourceId = map, abilityId = abilityId))
        var guard = 0
        while (guard++ < 40 && (driver.pendingDecision != null || driver.state.stack.isNotEmpty())) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    test("a single activation adds one landmark counter and does not transform") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val map = driver.putPermanentOnBattlefield(p1, "Treasure Map")

        activateAndResolve(driver, p1, map)

        landmarkCounters(driver, map) shouldBe 1
        cardName(driver, map) shouldBe "Treasure Map"
        driver.state.getEntity(map)!!.get<DoubleFacedComponent>()!!.currentFace shouldBe
            DoubleFacedComponent.Face.FRONT
    }

    test("the third landmark counter transforms into Treasure Cove and makes three Treasures") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val map = driver.putPermanentOnBattlefield(p1, "Treasure Map")
        // Seed two landmark counters so this single activation reaches the three-counter threshold.
        driver.addComponent(map, CountersComponent(mapOf(CounterType.LANDMARK to 2)))

        activateAndResolve(driver, p1, map)

        // Flipped to the back face; the three counters were removed as part of the flip.
        cardName(driver, map) shouldBe "Treasure Cove"
        driver.state.getEntity(map)!!.get<DoubleFacedComponent>()!!.currentFace shouldBe
            DoubleFacedComponent.Face.BACK
        landmarkCounters(driver, map) shouldBe 0

        // Exactly three Treasure tokens were created.
        val treasures = driver.getPermanents(p1).count { driver.getCardName(it) == "Treasure" }
        treasures shouldBe 3
    }
})
