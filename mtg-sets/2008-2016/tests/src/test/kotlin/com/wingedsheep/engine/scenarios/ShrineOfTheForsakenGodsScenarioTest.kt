package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.ShrineOfTheForsakenGods
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Shrine of the Forsaken Gods — the {C}{C} ability needs seven lands (itself included) and its mana
 * only pays for colorless spells.
 */
class ShrineOfTheForsakenGodsScenarioTest : FunSpec({

    val colorlessRelic = CardDefinition.artifact(name = "Shrine Colorless Relic", manaCost = ManaCost.parse("{2}"))
    val greenRelic = CardDefinition.artifact(name = "Shrine Green Relic", manaCost = ManaCost.parse("{1}{G}"))
    val bigAbility = ShrineOfTheForsakenGods.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(colorlessRelic, greenRelic))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.shrineWithLands(me: EntityId, otherLands: Int): EntityId {
        repeat(otherLands) { putLandOnBattlefield(me, "Island") }
        return putLandOnBattlefield(me, "Shrine of the Forsaken Gods")
    }

    test("with six lands the {C}{C} ability can't be activated") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val shrine = driver.shrineWithLands(me, 5)
        driver.submitExpectFailure(ActivateAbility(me, shrine, bigAbility))
    }

    test("with seven lands it adds {C}{C} that pays for a colorless spell") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val shrine = driver.shrineWithLands(me, 6)
        driver.submitSuccess(ActivateAbility(me, shrine, bigAbility))
        val relic = driver.putCardInHand(me, "Shrine Colorless Relic")

        val result = driver.submit(CastSpell(playerId = me, cardId = relic, paymentStrategy = PaymentStrategy.FromPool))
        result.outcome shouldBe Outcome.Done
        driver.state.getEntity(me)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 0
    }

    test("its mana can't pay a colored spell's generic cost") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val shrine = driver.shrineWithLands(me, 6)
        driver.submitSuccess(ActivateAbility(me, shrine, bigAbility))
        driver.giveMana(me, Color.GREEN, 1)
        val relic = driver.putCardInHand(me, "Shrine Green Relic")

        val result = driver.submit(CastSpell(playerId = me, cardId = relic, paymentStrategy = PaymentStrategy.FromPool))
        result.outcome shouldNotBe Outcome.Done
    }
})
