package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fem.cards.IcatianMoneychanger
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Icatian Moneychanger (Fallen Empires).
 *
 * The trap here is last-known information (CR 113.7a). The sacrifice is a *cost*, so by the time
 * "you gain 1 life for each credit counter on this creature" resolves, the Moneychanger is already
 * in the graveyard with its counters stripped — a live read gains nothing at all. This test exists
 * to keep that read on the last-known snapshot.
 */
class IcatianMoneychangerScenarioTest : FunSpec({

    val cashOut = IcatianMoneychanger.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(IcatianMoneychanger)
        return driver
    }

    test("sacrificing gains life for the credit counters it had, not zero") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val changer = driver.putCreatureOnBattlefield(alice, "Icatian Moneychanger")
        // Direct battlefield placement bypasses enters-with replacements, so stand the counters up
        // by hand: three from entering, plus two upkeeps survived.
        driver.replaceState(
            driver.state.updateEntity(changer) { c ->
                c.with(CountersComponent(mapOf(CounterType.CREDIT to 5)))
            }
        )
        driver.setLifeTotal(alice, 20)

        // The ability is upkeep-only and "your upkeep", so wait for Alice's own — the next upkeep
        // reached from her main phase belongs to the opponent.
        var guard = 0
        do {
            driver.passPriorityUntil(Step.UPKEEP)
            if (driver.activePlayer == alice) break
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        } while (guard++ < 4)
        driver.activePlayer shouldBe alice
        driver.submitSuccess(
            ActivateAbility(playerId = alice, sourceId = changer, abilityId = cashOut)
        )
        driver.bothPass()

        withClue("five credit counters cashed out for five life") {
            driver.getLifeTotal(alice) shouldBe 25
        }
        driver.state.getBattlefield().contains(changer) shouldBe false
    }
})
