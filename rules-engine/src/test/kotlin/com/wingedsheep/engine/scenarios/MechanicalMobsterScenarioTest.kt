package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.MechanicalMobster
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mechanical Mobster (SPM #168) — {3} Artifact Creature — Human Robot Villain 2/1.
 *   When this creature enters, exile up to one target card from a graveyard. Target creature you
 *   control connives.
 *
 * The ETB trigger carries two independent target requirements chosen together in one decision:
 * index 0 = the optional "up to one target card from a graveyard" exile, index 1 = the mandatory
 * "target creature you control" that connives. These tests prove the exile fires (and can be
 * declined), that the connive recipient is the chosen creature (not the Mobster), and that the
 * +1/+1 counter follows the nonland-discard rule. All primitives already exist (Move to exile,
 * Connive, Composite).
 */
class MechanicalMobsterScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(MechanicalMobster)
        d.initMirrorMatch(deck = Deck.of("Island" to 30), skipMulligans = true)
        return d
    }

    /**
     * Cast Mechanical Mobster, choose both targets (graveyard exile + connive recipient), then pause
     * on the connive discard decision. [exileTarget] selects the graveyard card (empty to decline);
     * [discardName] is placed in hand up front so the caller can discard it by id. Returns the
     * driver, the connive target, the pre-placed graveyard card, and the discard card's id.
     */
    fun enterAndConnive(
        discardName: String,
        exile: Boolean = true,
    ): DriverState {
        val d = driver()
        val you = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // The connive recipient: a separate creature you control, distinct from the Mobster.
        val creature = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        // A card sitting in a graveyard, eligible for the "up to one target card" exile.
        val graveyardCard = d.putCardInGraveyard(you, "Grizzly Bears")
        // Card we intend to discard (kept in hand across the cast); the connive draw pulls an Island.
        val toDiscard = d.putCardInHand(you, discardName)

        val mobster = d.putCardInHand(you, "Mechanical Mobster")
        repeat(3) { d.putLandOnBattlefield(you, "Island") }

        d.submit(
            CastSpell(
                playerId = you,
                cardId = mobster,
                paymentStrategy = PaymentStrategy.AutoPay,
            ),
        ).isSuccess shouldBe true
        d.bothPass() // resolve the creature; the ETB trigger goes on the stack and wants targets

        // One decision covers both requirements: index 0 = graveyard exile, index 1 = connive target.
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitMultiTargetSelection(
            you,
            mapOf(
                0 to if (exile) listOf(graveyardCard) else emptyList(),
                1 to listOf(creature),
            ),
        )

        // Resolve the ETB trigger off the stack; connive runs (draw), then pauses on the discard.
        d.bothPass()
        d.isPaused shouldBe true
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        return DriverState(d, creature, graveyardCard, toDiscard)
    }

    test("exiles the chosen graveyard card and connive discards a nonland to grow the target") {
        val s = enterAndConnive("Grizzly Bears")
        val you = s.driver.player1

        val decision = s.driver.pendingDecision as SelectCardsDecision
        s.driver.submitDecision(
            you,
            CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(s.discard)),
        )
        s.driver.isPaused shouldBe false

        // The targeted graveyard card is exiled.
        s.driver.getExile(you).contains(s.graveyardCard) shouldBe true
        s.driver.getGraveyard(you).contains(s.graveyardCard) shouldBe false
        // Discarding a nonland puts a +1/+1 counter on the connive target.
        s.driver.getGraveyard(you).contains(s.discard) shouldBe true
        val counters = s.driver.state.getEntity(s.creature)?.get<CountersComponent>()
        (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 1
    }

    test("declining the optional exile still connives; discarding a land adds no counter") {
        val s = enterAndConnive("Forest", exile = false)
        val you = s.driver.player1

        val decision = s.driver.pendingDecision as SelectCardsDecision
        s.driver.submitDecision(
            you,
            CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(s.discard)),
        )
        s.driver.isPaused shouldBe false

        // No graveyard card was exiled — it stays put.
        s.driver.getExile(you).contains(s.graveyardCard) shouldBe false
        s.driver.getGraveyard(you).contains(s.graveyardCard) shouldBe true
        // Discarding a land adds no counter.
        s.driver.getGraveyard(you).contains(s.discard) shouldBe true
        val counters = s.driver.state.getEntity(s.creature)?.get<CountersComponent>()
        (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
    }
})

private data class DriverState(
    val driver: GameTestDriver,
    val creature: EntityId,
    val graveyardCard: EntityId,
    val discard: EntityId,
)
