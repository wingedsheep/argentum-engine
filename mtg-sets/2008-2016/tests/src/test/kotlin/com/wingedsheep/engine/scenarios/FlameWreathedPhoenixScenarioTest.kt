package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bng.cards.FlameWreathedPhoenix
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Flame-Wreathed Phoenix (Born of the Gods #97) — {2}{R}{R} 3/3 flying, Tribute 2.
 * "When this creature enters, if tribute wasn't paid, it gains haste and
 * 'When this creature dies, return it to its owner's hand.'"
 */
class FlameWreathedPhoenixScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + FlameWreathedPhoenix)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast the Phoenix; the opponent answers the tribute question with the option starting [answer]. */
    fun GameTestDriver.castPhoenix(me: EntityId, answer: String): EntityId {
        giveMana(me, Color.RED, 2)
        giveColorlessMana(me, 2)
        castSpell(me, putCardInHand(me, "Flame-Wreathed Phoenix")).outcome shouldBe Outcome.Done
        bothPass()
        val decision = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe getOpponent(me)
        submitDecision(decision.playerId, OptionChosenResponse(decision.id, decision.options.indexOfFirst { it.startsWith(answer) }))
        return findPermanent(me, "Flame-Wreathed Phoenix")!!
    }

    fun GameTestDriver.destroy(me: EntityId, target: EntityId) {
        giveMana(me, Color.BLACK, 1)
        giveColorlessMana(me, 1)
        castSpell(me, putCardInHand(me, "Doom Blade"), listOf(target)).outcome shouldBe Outcome.Done
        bothPass()
    }

    fun GameTestDriver.plusOne(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("tribute paid: it enters as a 5/5 flyer without haste and stays dead when it dies") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val phoenix = driver.castPhoenix(me, "Pay tribute")

        driver.plusOne(phoenix) shouldBe 2
        driver.state.stack.size shouldBe 0
        driver.state.projectedState.getPower(phoenix) shouldBe 5
        driver.state.projectedState.hasKeyword(phoenix, Keyword.FLYING) shouldBe true
        driver.state.projectedState.hasKeyword(phoenix, Keyword.HASTE) shouldBe false

        driver.destroy(me, phoenix)
        driver.state.stack.size shouldBe 0
        driver.getGraveyardCardNames(me) shouldContain "Flame-Wreathed Phoenix"
        driver.findCardInHand(me, "Flame-Wreathed Phoenix").shouldBeNull()
    }

    test("tribute not paid: it keeps haste past this turn and returns to hand when it dies") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val phoenix = driver.castPhoenix(me, "Don't pay")

        driver.plusOne(phoenix) shouldBe 0
        driver.bothPass()
        driver.state.projectedState.hasKeyword(phoenix, Keyword.HASTE) shouldBe true

        // Neither gain has a duration — both outlive the turn.
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.state.projectedState.hasKeyword(phoenix, Keyword.HASTE) shouldBe true

        // It is now the opponent's turn; they destroy the Phoenix.
        driver.activePlayer shouldBe driver.getOpponent(me)
        driver.destroy(driver.getOpponent(me), phoenix)
        driver.bothPass() // resolve the granted dies trigger
        driver.state.stack.size shouldBe 0
        driver.findCardInHand(me, "Flame-Wreathed Phoenix").shouldNotBeNull()
        driver.getGraveyardCardNames(me) shouldNotContain "Flame-Wreathed Phoenix"
    }
})
