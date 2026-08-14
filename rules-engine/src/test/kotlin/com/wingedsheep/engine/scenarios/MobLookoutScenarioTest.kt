package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.MobLookout
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mob Lookout (SPM #136) — {1}{U/B} Creature — Human Rogue Villain 0/3.
 *   When this creature enters, target creature you control connives.
 *   (Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on that
 *   creature.)
 *
 * Proves the ETB trigger targets a chosen creature you control and runs the shared connive pipeline
 * with the +1/+1 counter landing on *that target* (not on Mob Lookout itself): discarding a nonland
 * grows the target; discarding a land does not.
 */
class MobLookoutScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(MobLookout)
        d.initMirrorMatch(deck = Deck.of("Island" to 30), skipMulligans = true)
        return d
    }

    /**
     * Cast Mob Lookout, target the pre-placed Grizzly Bears with the ETB connive, then pause on the
     * discard decision. [discardName] is placed in hand up front so the caller can discard it by id.
     * Returns the driver, the connive target, and the discard card's id.
     */
    fun enterAndConnive(discardName: String): Triple<GameTestDriver, EntityId, EntityId> {
        val d = driver()
        val you = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // The connive recipient: a separate creature you control, distinct from Mob Lookout.
        val target = d.putCreatureOnBattlefield(you, "Grizzly Bears")

        // Card we intend to discard (kept in hand across the cast); the connive draw pulls an Island.
        val toDiscard = d.putCardInHand(you, discardName)

        val lookout = d.putCardInHand(you, "Mob Lookout")
        repeat(2) { d.putLandOnBattlefield(you, "Island") }

        d.submit(
            CastSpell(
                playerId = you,
                cardId = lookout,
                paymentStrategy = PaymentStrategy.AutoPay,
            ),
        ).isSuccess shouldBe true
        d.bothPass() // resolve the creature; the ETB trigger goes on the stack and wants a target

        // Choose the Grizzly Bears as the creature that connives.
        d.submitTargetSelection(you, listOf(target))

        // Resolve the ETB trigger off the stack; connive runs (draw), then pauses on the discard.
        d.bothPass()

        // Connive resolves: draw, then a discard decision.
        d.isPaused shouldBe true
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        return Triple(d, target, toDiscard)
    }

    test("discarding a nonland puts a +1/+1 counter on the target creature") {
        val (d, target, nonland) = enterAndConnive("Grizzly Bears")
        val you = d.player1

        val decision = d.pendingDecision as SelectCardsDecision
        d.submitDecision(
            you,
            CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(nonland)),
        )
        d.isPaused shouldBe false

        d.getGraveyard(you).contains(nonland) shouldBe true
        val counters = d.state.getEntity(target)?.get<CountersComponent>()
        (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 1
    }

    test("discarding a land adds no counter") {
        val (d, target, land) = enterAndConnive("Forest")
        val you = d.player1

        val decision = d.pendingDecision as SelectCardsDecision
        d.submitDecision(
            you,
            CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(land)),
        )
        d.isPaused shouldBe false

        d.getGraveyard(you).contains(land) shouldBe true
        val counters = d.state.getEntity(target)?.get<CountersComponent>()
        (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
    }
})
