package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.DregRecycler
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Dreg Recycler (MOM #100): {T}, Sacrifice an artifact or creature: Each opponent loses 1 life
 * and you gain 1 life.
 */
class DregRecyclerScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("sacrificing a creature drains each opponent for 1") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        val recycler = driver.putCreatureOnBattlefield(p1, "Dreg Recycler")
        driver.removeSummoningSickness(recycler)
        val fodder = driver.putCreatureOnBattlefield(p1, "Dreg Recycler")

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = recycler,
                abilityId = DregRecycler.activatedAbilities.single().id,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
            )
        )
        result.error shouldBe null
        driver.bothPass()

        driver.getLifeTotal(p2) shouldBe 19
        driver.getLifeTotal(p1) shouldBe 21
        driver.state.getBattlefield().contains(fodder) shouldBe false
    }

    test("sacrificing an artifact also works") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        val recycler = driver.putCreatureOnBattlefield(p1, "Dreg Recycler")
        driver.removeSummoningSickness(recycler)
        val boulder = driver.putPermanentOnBattlefield(p1, "Runaway Boulder")

        val result = driver.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = recycler,
                abilityId = DregRecycler.activatedAbilities.single().id,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(boulder))
            )
        )
        result.error shouldBe null
        driver.bothPass()

        driver.getLifeTotal(p2) shouldBe 19
        driver.getLifeTotal(p1) shouldBe 21
    }
})
