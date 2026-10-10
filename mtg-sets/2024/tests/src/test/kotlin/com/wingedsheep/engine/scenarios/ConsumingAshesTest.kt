package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.ConsumingAshes
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Consuming Ashes: {2}{B}{B} Instant
 * Exile target creature. If it had mana value 3 or less, surveil 2.
 */
class ConsumingAshesTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ConsumingAshes))
        return driver
    }

    fun castAshes(driver: GameTestDriver): Pair<com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.giveMana(me, Color.BLACK, 2)
        driver.giveColorlessMana(me, 2)
        return me to opp
    }

    test("exiles a mana value 3 creature and surveils 2") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Island" to 30), startingLife = 20)
        val (me, opp) = castAshes(driver)

        // Centaur Courser is {2}{G} 3/3 — mana value 3, so surveil triggers.
        val courser = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val ashes = driver.putCardInHand(me, "Consuming Ashes")

        // Seed a known library top so surveil presents two cards.
        val top2 = driver.putCardOnTopOfLibrary(me, "Island")
        val top1 = driver.putCardOnTopOfLibrary(me, "Swamp")

        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = ashes,
                targets = listOf(ChosenTarget.Permanent(courser)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        (result.error == null) shouldBe true
        driver.bothPass()

        // Creature is exiled.
        driver.getExile(opp).contains(courser) shouldBe true

        // Surveil 2 pauses for the keep/graveyard choice.
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val select = driver.pendingDecision as SelectCardsDecision
        select.options.size shouldBe 2

        // Mill the top card to the graveyard, keep the second on top.
        driver.submitDecision(me, CardsSelectedResponse(decisionId = select.id, selectedCards = listOf(top1)))

        // One card stays on top and the surveil just showed it: no order prompt.
        driver.isPaused shouldBe false

        driver.getGraveyard(me).contains(top1) shouldBe true
        val lib = driver.state.getZone(ZoneKey(me, Zone.LIBRARY))
        lib[0] shouldBe top2
    }

    test("exiles a high mana value creature without surveiling") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Island" to 30), startingLife = 20)
        val (me, opp) = castAshes(driver)

        // Force of Nature is {3}{G}{G} — mana value 5, so no surveil.
        val beast = driver.putCreatureOnBattlefield(opp, "Force of Nature")
        val ashes = driver.putCardInHand(me, "Consuming Ashes")

        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = ashes,
                targets = listOf(ChosenTarget.Permanent(beast)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        (result.error == null) shouldBe true
        driver.bothPass()

        // Creature is exiled, and no surveil decision is pending.
        driver.getExile(opp).contains(beast) shouldBe true
        driver.isPaused shouldBe false
    }
})
