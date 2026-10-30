package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.engine.core.Outcome

/**
 * Scenario test for World War Hulk (MSH #197) — {3}{G}{G} Enchantment — Saga.
 *
 *   I — The next red or green creature spell you cast this turn can be cast without paying its
 *       mana cost.
 *   II — Put three +1/+1 counters on target creature you control.
 *   III — Choose target creature you control. Until end of turn, double its power and toughness
 *         and it gains trample.
 *
 * Chapter I is the new vocabulary ([com.wingedsheep.sdk.dsl.Effects.GrantNextSpellFreeCast]); the
 * filter (red **or green**, and **creature**) and the "next spell" consumption rule are the parts
 * that can silently play wrong, so both are exercised here on the real card. The primitive's own
 * edge cases (turn expiry, source in the graveyard, the once-per-turn interaction) live in
 * [GrantFreeCastThisTurnTest].
 */
class WorldWarHulkScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Drain the stack, auto-answering anything that pauses. */
    fun GameTestDriver.drain() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 60) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
            guard++
        }
    }

    /** Drain the stack, answering every target request with [targets]. */
    fun GameTestDriver.drainTargeting(chooser: EntityId, targets: List<EntityId>) {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 60) {
            val decision = state.pendingDecision
            when {
                decision is ChooseTargetsDecision -> submitTargetSelection(chooser, targets)
                decision != null -> autoResolveDecision()
                else -> bothPass()
            }
            guard++
        }
    }

    /**
     * Advance to the precombat main of the starting player's [nth] turn — the clock the Saga's
     * lore counters run on. `turnNumber` counts player turns and this is a duel, so the starting
     * player's nth turn is turn `2n - 1`.
     */
    fun GameTestDriver.advanceToMain(nth: Int) {
        val targetTurn = nth * 2 - 1
        var guard = 0
        while (!(state.turnNumber == targetTurn && state.step == Step.PRECOMBAT_MAIN) && guard < 500) {
            if (state.gameOver) throw AssertionError("Game ended while advancing to turn $targetTurn")
            when {
                state.pendingDecision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> bothPass()
                else -> break
            }
            guard++
        }
        if (guard >= 500) error("Failed to reach turn $targetTurn precombat main")
    }

    /** Cast the Saga and let chapter I resolve, leaving a pending free-cast rider. */
    fun GameTestDriver.castSaga(controller: EntityId): EntityId {
        giveMana(controller, Color.GREEN, 5)
        val saga = putCardInHand(controller, "World War Hulk")
        castSpell(controller, saga)
        drain()
        return saga
    }

    fun GameTestDriver.plusOnePlusOne(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("chapter I: a green creature spell can be cast without paying its mana cost") {
        val driver = createDriver()
        val controller = driver.activePlayer!!

        driver.castSaga(controller)
        withClue("chapter I installs one pending free-cast rider") {
            driver.state.pendingFreeCastSpells.size shouldBe 1
            driver.state.pendingFreeCastSpells.single().controllerId shouldBe controller
        }

        // Grizzly Bears is {1}{G}; the pool is empty after paying for the Saga, so this cast is
        // free or it doesn't happen at all.
        val bears = driver.putCardInHand(controller, "Grizzly Bears")
        driver.submit(
            CastSpell(controller, bears, useWithoutPayingManaCost = true, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.findPermanent(controller, "Grizzly Bears") shouldNotBe null
        withClue("the rider is spent by the spell it paid for") {
            driver.state.pendingFreeCastSpells.shouldBeEmpty()
        }
    }

    test("chapter I: the legal-action enumerator offers the free cast, filtered to red or green creatures") {
        val driver = createDriver()
        val controller = driver.activePlayer!!

        driver.castSaga(controller)

        val bears = driver.putCardInHand(controller, "Grizzly Bears")
        val warrior = driver.putCardInHand(controller, "Phantom Warrior")
        val bolt = driver.putCardInHand(controller, "Lightning Bolt")

        // The enumerator half of the permission — `hasFreeCastPermission` is reached through
        // `EnumerationContext.freeCastPermissionFor` here, not through `CastSpellHandler.validate`.
        // This is the path a real player and the AI see, and it must agree with validation or the
        // action is offered and then rejected.
        val freeCasts = driver.legalActions(controller)
            .filter { it.actionType == "CastWithoutPayingManaCost" }
            .map { it.action as CastSpell }

        withClue("the pool is empty after the Saga, so only the rider can offer this cast") {
            freeCasts.any { it.cardId == bears && it.useWithoutPayingManaCost } shouldBe true
        }
        withClue("Phantom Warrior is a blue creature — outside the filter") {
            freeCasts.any { it.cardId == warrior } shouldBe false
        }
        withClue("Lightning Bolt is red but not a creature spell") {
            freeCasts.any { it.cardId == bolt } shouldBe false
        }

        // And the offered action is actually accepted by the handler.
        driver.submit(freeCasts.single { it.cardId == bears }).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.findPermanent(controller, "Grizzly Bears") shouldNotBe null
    }

    test("chapter I: a red creature spell qualifies too, and a blue one does not") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val costCalculator = CostCalculator(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

        driver.castSaga(controller)

        withClue("Phantom Warrior is a blue creature — outside the red-or-green filter") {
            costCalculator.hasFreeCastPermission(
                driver.state,
                controller,
                driver.cardRegistry.requireCard("Phantom Warrior")
            ) shouldBe false
        }
        withClue("Lightning Bolt is red but not a creature spell") {
            costCalculator.hasFreeCastPermission(
                driver.state,
                controller,
                driver.cardRegistry.requireCard("Lightning Bolt")
            ) shouldBe false
        }
        withClue("Goblin Guide is a red creature — inside the filter") {
            costCalculator.hasFreeCastPermission(
                driver.state,
                controller,
                driver.cardRegistry.requireCard("Goblin Guide")
            ) shouldBe true
        }

        // A blue creature spell cast in between must not consume the rider.
        driver.giveMana(controller, Color.BLUE, 3)
        val warrior = driver.putCardInHand(controller, "Phantom Warrior")
        driver.castSpell(controller, warrior).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.findPermanent(controller, "Phantom Warrior") shouldNotBe null
        withClue("a non-matching spell leaves the rider alone") {
            driver.state.pendingFreeCastSpells.size shouldBe 1
        }

        // The red creature spell is still free.
        val guide = driver.putCardInHand(controller, "Goblin Guide")
        driver.submit(
            CastSpell(controller, guide, useWithoutPayingManaCost = true, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.findPermanent(controller, "Goblin Guide") shouldNotBe null
        driver.state.pendingFreeCastSpells.shouldBeEmpty()
    }

    test("chapter I: a matching spell cast for full price is 'the next' one and spends the grant") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val costCalculator = CostCalculator(driver.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

        driver.castSaga(controller)

        // Pay for the first green creature spell in full — the grant applied to it either way.
        driver.giveMana(controller, Color.GREEN, 2)
        val bears = driver.putCardInHand(controller, "Grizzly Bears")
        driver.castSpell(controller, bears).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.findPermanent(controller, "Grizzly Bears") shouldNotBe null

        withClue("the grant named a spell, not a discount — it is spent") {
            driver.state.pendingFreeCastSpells.shouldBeEmpty()
            costCalculator.hasFreeCastPermission(
                driver.state,
                controller,
                driver.cardRegistry.requireCard("Goblin Guide")
            ) shouldBe false
        }

        // A second matching spell gets no free cast.
        val guide = driver.putCardInHand(controller, "Goblin Guide")
        driver.submit(
            CastSpell(controller, guide, useWithoutPayingManaCost = true, paymentStrategy = PaymentStrategy.FromPool)
        ).outcome shouldNotBe Outcome.Done
    }

    test("chapters II and III: three +1/+1 counters, then doubled power/toughness and trample") {
        val driver = createDriver()
        val controller = driver.activePlayer!!

        driver.castSaga(controller)
        val bear = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")

        // Chapter II on the controller's second turn.
        driver.advanceToMain(2)
        driver.drainTargeting(controller, listOf(bear))
        withClue("chapter II puts three +1/+1 counters on the targeted creature") {
            driver.plusOnePlusOne(bear) shouldBe 3
            driver.state.projectedState.getPower(bear) shouldBe 5
            driver.state.projectedState.getToughness(bear) shouldBe 5
        }

        // Chapter III on the third turn — doubling reads the counter-boosted 5/5.
        driver.advanceToMain(3)
        driver.drainTargeting(controller, listOf(bear))
        withClue("chapter III doubles the creature's current power and toughness and grants trample") {
            driver.state.projectedState.getPower(bear) shouldBe 10
            driver.state.projectedState.getToughness(bear) shouldBe 10
            driver.state.projectedState.hasKeyword(bear, Keyword.TRAMPLE) shouldBe true
        }
        withClue("the Saga is sacrificed after chapter III") {
            driver.findPermanent(controller, "World War Hulk") shouldBe null
        }

        // The doubling and the trample are until end of turn; the counters are not.
        driver.advanceToMain(4)
        withClue("only the +1/+1 counters survive the turn") {
            driver.state.projectedState.getPower(bear) shouldBe 5
            driver.state.projectedState.getToughness(bear) shouldBe 5
            driver.state.projectedState.hasKeyword(bear, Keyword.TRAMPLE) shouldBe false
        }
    }
})
