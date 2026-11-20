package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bng.cards.NessianDemolok
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Nessian Demolok (Born of the Gods #128) — {3}{G}{G} 3/3, Tribute 3.
 * "When this creature enters, if tribute wasn't paid, destroy target noncreature permanent."
 *
 * The first tribute card whose not-paid trigger targets: paying tribute must keep the trigger off
 * the stack entirely (no target prompt), and declining must offer only noncreature permanents.
 */
class NessianDemolokScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + NessianDemolok)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast the Demolok; the opponent answers the tribute question with the option starting [answer]. */
    fun GameTestDriver.castDemolok(me: EntityId, answer: String): EntityId {
        giveMana(me, Color.GREEN, 2)
        giveColorlessMana(me, 3)
        castSpell(me, putCardInHand(me, "Nessian Demolok")).outcome shouldBe Outcome.Done
        bothPass()
        val decision = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe getOpponent(me)
        submitDecision(decision.playerId, OptionChosenResponse(decision.id, decision.options.indexOfFirst { it.startsWith(answer) }))
        return findPermanent(me, "Nessian Demolok")!!
    }

    fun GameTestDriver.plusOne(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("tribute paid: it enters as a 6/6 and the destroy trigger never goes on the stack") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val enchantment = driver.putPermanentOnBattlefield(driver.getOpponent(me), "Test Enchantment")

        val demolok = driver.castDemolok(me, "Pay tribute")

        driver.plusOne(demolok) shouldBe 3
        driver.state.projectedState.getPower(demolok) shouldBe 6
        driver.pendingDecision.shouldBeNull()
        driver.state.stack.size shouldBe 0
        driver.state.getEntity(enchantment).shouldNotBeNull()
        driver.findPermanent(driver.getOpponent(me), "Test Enchantment") shouldBe enchantment
    }

    test("tribute not paid: it stays a 3/3 and destroys target noncreature permanent, not a creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val enchantment = driver.putPermanentOnBattlefield(opponent, "Test Enchantment")
        val bears = driver.putPermanentOnBattlefield(opponent, "Centaur Courser")

        val demolok = driver.castDemolok(me, "Don't pay")
        driver.plusOne(demolok) shouldBe 0

        val targeting = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = targeting.legalTargets.values.flatten()
        legal shouldContain enchantment
        legal.contains(bears) shouldBe false
        legal.contains(demolok) shouldBe false

        driver.submitTargetSelection(me, listOf(enchantment))
        driver.bothPass()

        driver.state.stack.size shouldBe 0
        driver.findPermanent(opponent, "Test Enchantment").shouldBeNull()
        driver.getGraveyardCardNames(opponent) shouldContain "Test Enchantment"
        driver.findPermanent(opponent, "Centaur Courser") shouldBe bears
    }
})
