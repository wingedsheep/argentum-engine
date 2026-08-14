package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.woe.cards.VirtueOfLoyalty
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class VirtueOfLoyaltyScenarioTest : FunSpec({

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.registerCard(VirtueOfLoyalty)
        it.initMirrorMatch(Deck.of("Plains" to 30, "Grizzly Bears" to 30))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("Ardenvale Fealty creates a vigilant Knight and leaves the card on an Adventure") {
        val driver = driver()
        val player = driver.activePlayer!!
        val virtue = driver.putCardInHand(player, "Virtue of Loyalty")
        driver.giveMana(player, Color.WHITE, 2)

        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = virtue,
                faceIndex = 0,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        while (driver.stackSize > 0) driver.bothPass()

        driver.getExile(player) shouldContain virtue
        val knight = driver.getPermanents(player).single {
            driver.state.projectedState.hasSubtype(it, "Knight")
        }
        driver.state.projectedState.getPower(knight) shouldBe 2
        driver.state.projectedState.getToughness(knight) shouldBe 2
        driver.state.projectedState.hasKeyword(knight, Keyword.VIGILANCE) shouldBe true
    }

    test("end-step trigger puts a counter on and untaps each creature you control") {
        val driver = driver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Virtue of Loyalty")
        val first = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val second = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.replaceState(driver.state.updateEntity(first) { it.with(TappedComponent) })

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        for (creature in listOf(first, second)) {
            driver.state.getEntity(creature)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            driver.state.getEntity(creature)?.get<TappedComponent>() shouldBe null
        }
    }
})
