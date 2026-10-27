package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.support.answerForcedPayments
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Power Leak.
 *
 * Enchant enchantment. At the beginning of the upkeep of enchanted enchantment's controller, that
 * player may pay any amount of mana. This Aura deals 2 damage to that player. Prevent X of that
 * damage, where X is the amount of mana that player paid this way.
 *
 * The trigger is the Aura's (its controller controls it), but the step, the payment and the
 * damage all belong to the enchanted enchantment's controller. X is real prevention: a
 * single-instance shield from this Aura, spent by the 2 damage even when X exceeds it.
 */
class PowerLeakScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun attach(driver: GameTestDriver, auraId: EntityId, hostId: EntityId) {
        driver.addComponent(auraId, AttachedToComponent(hostId))
        val existing = driver.state.getEntity(hostId)?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        driver.addComponent(hostId, AttachmentsComponent(existing + auraId))
    }

    data class Board(val driver: GameTestDriver, val me: EntityId, val victim: EntityId, val islands: List<EntityId>)

    /**
     * My Power Leak on the victim's enchantment; the victim has [islands] untapped Islands and I
     * have no mana at all, so a prompt routed to the wrong player would never appear. Advances to
     * the victim's upkeep with the trigger on the stack.
     */
    fun setUp(islands: Int): Board {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val victim = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val circle = driver.putPermanentOnBattlefield(victim, "Circle of Protection: Red")
        val leak = driver.putPermanentOnBattlefield(me, "Power Leak")
        attach(driver, leak, circle)
        val lands = List(islands) { driver.putLandOnBattlefield(victim, "Island") }

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe victim
        return Board(driver, me, victim, lands)
    }

    fun resolveAndPay(board: Board, x: Int) {
        val driver = board.driver
        driver.bothPass()
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseNumberDecision>()
        withClue("the enchanted enchantment's controller chooses and pays, up to their own mana") {
            decision.playerId shouldBe board.victim
            decision.minValue shouldBe 0
            decision.maxValue shouldBe board.islands.size
        }
        driver.submitDecision(board.victim, NumberChosenResponse(decision.id, x)).error shouldBe null
    }

    fun tappedIslands(board: Board) = board.islands.count { board.driver.state.getEntity(it)?.has<TappedComponent>() == true }

    fun noShieldLeft(board: Board) = board.driver.state.floatingEffects.none {
        it.effect.modification is SerializableModification.PreventNextDamageInstanceFromSource
    }

    test("triggers on the enchanted enchantment's controller's upkeep, controlled by the Aura's controller") {
        val board = setUp(islands = 2)
        val trigger = board.driver.state.getEntity(board.driver.state.stack.last())
            ?.get<TriggeredAbilityOnStackComponent>()
        withClue("one trigger, and it is the Aura controller's") {
            board.driver.state.stack.size shouldBe 1
            trigger?.controllerId shouldBe board.me
        }
    }

    test("paying 0 — the full 2 damage") {
        val board = setUp(islands = 2)
        resolveAndPay(board, 0)
        board.driver.getLifeTotal(board.victim) shouldBe 18
        board.driver.getLifeTotal(board.me) shouldBe 20
        tappedIslands(board) shouldBe 0
    }

    test("paying 1 — 1 damage prevented, 1 dealt") {
        val board = setUp(islands = 2)
        resolveAndPay(board, 1)
        board.driver.getLifeTotal(board.victim) shouldBe 19
        tappedIslands(board) shouldBe 1
        noShieldLeft(board) shouldBe true
    }

    test("paying 2 — all prevented") {
        val board = setUp(islands = 2)
        resolveAndPay(board, 2)
        board.driver.getLifeTotal(board.victim) shouldBe 20
        tappedIslands(board) shouldBe 2
        noShieldLeft(board) shouldBe true
    }

    test("overpaying — all prevented, and the surplus does not linger as a shield") {
        val board = setUp(islands = 4)
        resolveAndPay(board, 4)
        board.driver.getLifeTotal(board.victim) shouldBe 20
        tappedIslands(board) shouldBe 4
        withClue("the shield was spent by the 2-damage instance, not left with 2 to spare") {
            noShieldLeft(board) shouldBe true
        }
    }

    test("with no mana to pay, refusing is the only answer and the full 2 damage is dealt") {
        val board = setUp(islands = 0)
        board.driver.bothPass()
        board.driver.answerForcedPayments()
        board.driver.pendingDecision shouldBe null
        board.driver.getLifeTotal(board.victim) shouldBe 18
    }

    test("does not trigger on the Aura controller's own upkeep") {
        val board = setUp(islands = 2)
        val driver = board.driver
        resolveAndPay(board, 0)
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe board.me

        withClue("my upkeep: I don't control the enchanted enchantment, so nothing triggers") {
            driver.state.stack.size shouldBe 0
            driver.pendingDecision shouldBe null
            driver.getLifeTotal(board.me) shouldBe 20
            driver.getLifeTotal(board.victim) shouldBe 18
        }
    }
})
