package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.BubbleSmuggler
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Bubble Smuggler (MKM #41) — `disguiseFaceUpEffect`, the disguise-side sibling of
 * `morphFaceUpEffect`.
 *
 * Disguise {5}{U}
 * As this creature is turned face up, put four +1/+1 counters on it.
 *
 * The clause is a **replacement**, not a trigger: it applies inside the turn-up special action
 * (CR 701.34a), which doesn't use the stack and can't be responded to. That's what these tests pin
 * down — the counters are already there the instant the action resolves, with nothing on the stack
 * and no pending decision, so an opponent never gets a window against the 2/1 body.
 *
 * Covers: the face-down 2/2 carries no counters; the flip places exactly four and yields a 6/5;
 * the placement uses no stack; and a Bubble Smuggler cast normally for {1}{U} gets nothing.
 */
class BubbleSmugglerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BubbleSmuggler))
        return driver
    }

    fun counterCount(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Put Bubble Smuggler onto the battlefield face down, deriving turn-up data as a real entry does. */
    fun GameTestDriver.putFaceDown(playerId: EntityId, mode: FaceDownMode): EntityId {
        val id = putCreatureOnBattlefield(playerId, "Bubble Smuggler")
        val cardDef = cardRegistry.requireCard("Bubble Smuggler")
        replaceState(
            state.updateEntity(id) { container ->
                var c = container.with(FaceDownComponent).with(FaceDownModeComponent(mode))
                FaceDownTurnUp.dataFor(cardDef, "Bubble Smuggler", mode)?.let { c = c.with(it) }
                c
            }
        )
        removeSummoningSickness(id)
        return id
    }

    test("the derived disguise turn-up procedure carries the face-up effect") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val smuggler = driver.putFaceDown(player, FaceDownMode.DISGUISE)

        val procedure = driver.state.getEntity(smuggler)
            ?.get<com.wingedsheep.engine.state.components.identity.MorphDataComponent>()
            ?.procedures?.single()
        procedure.shouldNotBeNull()
        procedure.mechanic shouldBe FaceDownMode.DISGUISE
        procedure.cost.description shouldBe "{5}{U}"
        procedure.faceUpEffect.shouldNotBeNull()
    }

    test("while face down it is a plain 2/2 with no counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val smuggler = driver.putFaceDown(player, FaceDownMode.DISGUISE)

        counterCount(driver, smuggler) shouldBe 0
        driver.state.projectedState.getPower(smuggler) shouldBe 2
        driver.state.projectedState.getToughness(smuggler) shouldBe 2
    }

    test("turning it face up puts four +1/+1 counters on it, making it a 6/5") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val smuggler = driver.putFaceDown(player, FaceDownMode.DISGUISE)
        driver.giveMana(player, Color.BLUE, 6) // Disguise {5}{U}

        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = smuggler,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null

        driver.state.getEntity(smuggler)?.get<FaceDownComponent>() shouldBe null
        counterCount(driver, smuggler) shouldBe 4
        // Printed 2/1 plus four counters.
        driver.state.projectedState.getPower(smuggler) shouldBe 6
        driver.state.projectedState.getToughness(smuggler) shouldBe 5
    }

    test("the counters land inside the special action — nothing goes on the stack") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val smuggler = driver.putFaceDown(player, FaceDownMode.DISGUISE)
        driver.giveMana(player, Color.BLUE, 6)

        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = smuggler,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null

        // No trigger to resolve and no decision to answer: a replacement effect, not a trigger.
        // The opponent never gets priority with a 2/1 Bubble Smuggler on the battlefield.
        driver.stackSize shouldBe 0
        driver.pendingDecision shouldBe null
        counterCount(driver, smuggler) shouldBe 4
    }

    test("cast face up for its mana cost, it is a plain 2/1 with no counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(player, "Bubble Smuggler")
        driver.giveMana(player, Color.BLUE, 2) // {1}{U}

        driver.castSpell(player, card).error shouldBe null
        driver.bothPass()

        val smuggler = driver.findPermanent(player, "Bubble Smuggler")
        smuggler.shouldNotBeNull()
        counterCount(driver, smuggler) shouldBe 0
        driver.state.projectedState.getPower(smuggler) shouldBe 2
        driver.state.projectedState.getToughness(smuggler) shouldBe 1
    }
})
