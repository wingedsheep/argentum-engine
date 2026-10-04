package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.TheCreationOfAvacyn
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The Creation of Avacyn (MH3 #86) — {1}{B}{B} Enchantment — Saga.
 *
 *  I — Search your library for a card, exile it face down, then shuffle.
 *  II — Turn the exiled card face up. If it's a creature card, you lose life equal to its mana value.
 *  III — You may put the exiled card onto the battlefield if it's a creature card. If you don't put
 *        it onto the battlefield, put it into its owner's hand.
 */
class TheCreationOfAvacynScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TheCreationOfAvacyn))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        return driver
    }

    /** Pass until turn [turn] reaches [step], auto-answering incidental decisions (cleanup discards). */
    fun GameTestDriver.advanceTo(turn: Int, step: Step) {
        var guard = 0
        while (!(state.turnNumber == turn && state.step == step) && guard++ < 500) {
            if (state.gameOver) throw AssertionError("Game ended while advancing to turn $turn $step")
            val decision = state.pendingDecision
            when {
                decision is ChooseTargetsDecision -> return
                decision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> bothPass()
                else -> break
            }
        }
    }

    fun GameTestDriver.passUntilDecisionOrEmptyStack() {
        var guard = 0
        while (state.pendingDecision == null && state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    fun GameTestDriver.isFaceDown(id: EntityId): Boolean = state.getEntity(id)?.has<FaceDownComponent>() == true

    /** Cast the Saga and answer chapter I by exiling [pick] from the library. */
    fun GameTestDriver.castSagaAndExile(player: EntityId, pick: EntityId) {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(player, Color.BLACK, 3)
        val saga = putCardInHand(player, "The Creation of Avacyn")
        castSpell(player, saga)
        passUntilDecisionOrEmptyStack()
        state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        submitCardSelection(player, listOf(pick))
        resolveStack()
    }

    test("creature card: exiled face down, turned up for life loss, then put onto the battlefield") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bears = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")

        driver.castSagaAndExile(me, bears)
        withClue("chapter I exiles the found card face down") {
            driver.getExile(me) shouldContain bears
            driver.isFaceDown(bears) shouldBe true
            driver.state.getLibrary(me) shouldNotContain bears
        }
        driver.getLifeTotal(me) shouldBe 20

        // Turn 3: lore 2 → chapter II turns it face up; Grizzly Bears (MV 2) costs 2 life.
        driver.advanceTo(3, Step.PRECOMBAT_MAIN)
        driver.resolveStack()
        withClue("chapter II turns the exiled card face up") {
            driver.getExile(me) shouldContain bears
            driver.isFaceDown(bears) shouldBe false
        }
        driver.getLifeTotal(me) shouldBe 18

        // Turn 5: lore 3 → chapter III offers to put the creature onto the battlefield.
        driver.advanceTo(5, Step.PRECOMBAT_MAIN)
        driver.passUntilDecisionOrEmptyStack()
        driver.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.resolveStack()

        withClue("the creature enters the battlefield under my control") {
            driver.findPermanent(me, "Grizzly Bears") shouldNotBe null
            driver.getExile(me) shouldNotContain bears
            driver.getHand(me) shouldNotContain bears
        }
        withClue("the Saga is sacrificed after chapter III") {
            driver.findPermanent(me, "The Creation of Avacyn") shouldBe null
        }
    }

    test("declining chapter III puts the creature card into its owner's hand") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bears = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")

        driver.castSagaAndExile(me, bears)
        driver.advanceTo(5, Step.PRECOMBAT_MAIN)
        driver.passUntilDecisionOrEmptyStack()
        driver.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)
        driver.resolveStack()

        driver.getHand(me) shouldContain bears
        driver.getExile(me) shouldNotContain bears
        driver.findPermanent(me, "Grizzly Bears") shouldBe null
    }

    test("noncreature card: no life loss, no battlefield option, goes to hand") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val swamp = driver.state.getLibrary(me).first()

        driver.castSagaAndExile(me, swamp)
        driver.isFaceDown(swamp) shouldBe true

        driver.advanceTo(3, Step.PRECOMBAT_MAIN)
        driver.resolveStack()
        withClue("turned face up, but a land card costs no life") {
            driver.isFaceDown(swamp) shouldBe false
            driver.getLifeTotal(me) shouldBe 20
        }

        driver.advanceTo(5, Step.PRECOMBAT_MAIN)
        driver.passUntilDecisionOrEmptyStack()
        withClue("a noncreature card offers no battlefield choice") {
            (driver.state.pendingDecision is YesNoDecision) shouldBe false
        }
        driver.resolveStack()
        driver.getHand(me) shouldContain swamp
        driver.getExile(me) shouldNotContain swamp
        driver.findPermanent(me, "The Creation of Avacyn") shouldBe null
    }
})
