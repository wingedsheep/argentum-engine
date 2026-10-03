package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.InvestigatedEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.InvestigatedThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Investigate (CR 701.16a) as an observable action: `InvestigateEffect` emits one
 * [InvestigatedEvent] per investigate, `firstThisTurn` only on the investigating player's first one
 * this turn, and "create a Clue token" emits none.
 */
class InvestigateTriggerTest : FunSpec({

    val clue = card("Clue") { typeLine = "Artifact — Clue" }

    // "Whenever you investigate, you gain 1 life." — counts every investigate.
    val watcher = card("Test Investigate Watcher") {
        manaCost = "{1}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.investigates()
            effect = Effects.GainLife(1)
        }
    }

    fun spell(name: String, effect: com.wingedsheep.sdk.scripting.effects.Effect) = card(name) {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { this.effect = effect }
    }

    val investigateTwice = spell("Test Investigate Twice", Effects.Investigate(2))
    val investigateZero = spell("Test Investigate Zero", Effects.Investigate(0))
    val createClue = spell("Test Create Clue", Effects.CreateClue())
    // "Target player investigates." (Panther Pounce)
    val targetPlayerInvestigates = card("Test Target Player Investigates") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val player = target(Targets.Player)
            effect = Effects.Investigate(controller = player)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(clue, watcher, investigateTwice, investigateZero, createClue, targetPlayerInvestigates))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.clues(playerId: EntityId): Int =
        getPermanents(playerId).count { state.getEntity(it)?.get<CardComponent>()?.name == "Clue" }

    /** Cast [name], resolve it, and return the investigate events its resolution emitted. */
    fun GameTestDriver.castAndResolve(
        playerId: EntityId,
        name: String,
        targets: List<EntityId> = emptyList()
    ): List<InvestigatedEvent> {
        val id = putCardInHand(playerId, name)
        giveMana(playerId, Color.BLUE, 1)
        castSpell(playerId, id, targets).outcome shouldBe Outcome.Done
        val resolved = bothPass()
        val investigated = resolved.events.filterIsInstance<InvestigatedEvent>()
        while (state.stack.isNotEmpty()) bothPass()
        return investigated
    }

    test("investigate twice is two investigates; only the first is the turn's first") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Test Investigate Watcher")

        val events = driver.castAndResolve(me, "Test Investigate Twice")

        events.map { it.playerId } shouldBe listOf(me, me)
        events.map { it.firstThisTurn } shouldBe listOf(true, false)
        driver.clues(me) shouldBe 2
        driver.getLifeTotal(me) shouldBe 22 // the watcher triggered once per investigate
        driver.state.getEntity(me)?.has<InvestigatedThisTurnComponent>() shouldBe true

        driver.castAndResolve(me, "Test Investigate Twice").map { it.firstThisTurn } shouldBe listOf(false, false)
    }

    test("creating a Clue token is not investigating") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Test Investigate Watcher")

        driver.castAndResolve(me, "Test Create Clue").shouldBeEmpty()

        driver.clues(me) shouldBe 1
        driver.getLifeTotal(me) shouldBe 20
        driver.state.getEntity(me)?.has<InvestigatedThisTurnComponent>() shouldBe false
    }

    test("investigating zero times investigates not at all") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.castAndResolve(me, "Test Investigate Zero").shouldBeEmpty()

        driver.clues(me) shouldBe 0
        driver.state.getEntity(me)?.has<InvestigatedThisTurnComponent>() shouldBe false
    }

    test("the player who gets the Clue is the one who investigated") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Test Investigate Watcher")

        val events = driver.castAndResolve(me, "Test Target Player Investigates", listOf(opponent))

        events.map { it.playerId } shouldBe listOf(opponent)
        driver.clues(opponent) shouldBe 1
        driver.getLifeTotal(me) shouldBe 20 // my "whenever you investigate" didn't fire
        driver.state.getEntity(opponent)?.has<InvestigatedThisTurnComponent>() shouldBe true
        driver.state.getEntity(me)?.has<InvestigatedThisTurnComponent>() shouldBe false
    }

    test("the first-this-turn marker clears at end of turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.castAndResolve(me, "Test Investigate Twice")
        driver.state.getEntity(me)?.has<InvestigatedThisTurnComponent>() shouldBe true

        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe driver.getOpponent(me)
        driver.state.getEntity(me)?.has<InvestigatedThisTurnComponent>() shouldBe false
    }
})
