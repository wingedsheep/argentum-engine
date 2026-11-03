package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.TheWrathOfKhan
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The Wrath of Khan — {2}{R} Enchantment — Saga (TRK #181).
 *
 * I — This Saga deals 4 damage to target creature an opponent controls. At the beginning of the
 *     next combat this turn, gain control of that creature until end of turn. Untap it. It gains
 *     haste until end of turn.
 *
 * Chapter I's delayed threaten acts on the creature chosen when the chapter resolved.
 */
class TheWrathOfKhanScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TheWrathOfKhan))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.drain() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 60) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
            guard++
        }
    }

    fun GameTestDriver.castSagaAt(me: EntityId, creature: EntityId) {
        giveMana(me, Color.RED, 3)
        val card = putCardInHand(me, "The Wrath of Khan")
        submit(CastSpell(playerId = me, cardId = card)).error shouldBe null
        bothPass() // the Saga resolves and chapter I triggers
        if (state.pendingDecision != null) submitTargetSelection(me, listOf(creature))
        drain()
    }

    test("chapter I deals 4, then at the next combat steals, untaps and hastes the creature") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val force = driver.putCreatureOnBattlefield(opponent, "Force of Nature")
        driver.tapPermanent(force)

        driver.castSagaAt(me, force)

        withClue("4 damage marked, still the opponent's before combat") {
            driver.state.getEntity(force)?.get<com.wingedsheep.engine.state.components.battlefield.DamageComponent>()
                ?.amount shouldBe 4
            driver.state.projectedState.getController(force) shouldBe opponent
        }

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.drain()

        withClue("the delayed trigger took the creature, untapped it and gave it haste") {
            driver.state.projectedState.getController(force) shouldBe me
            driver.isTapped(force) shouldBe false
            driver.state.projectedState.hasKeyword(force, Keyword.HASTE) shouldBe true
        }
    }

    test("a creature the 4 damage killed is not stolen") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.castSagaAt(me, bears)
        driver.getGraveyardCardNames(opponent).contains("Grizzly Bears") shouldBe true

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.drain()

        withClue("nothing changed hands") {
            driver.findPermanent(me, "Grizzly Bears") shouldBe null
            driver.getGraveyardCardNames(opponent).contains("Grizzly Bears") shouldBe true
        }
    }

    test("chapter I after combat steals nothing at a later turn's combat") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val force = driver.putCreatureOnBattlefield(opponent, "Force of Nature")

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.castSagaAt(me, force)

        withClue("no combat is left this turn, so the threaten is dropped at end of turn") {
            driver.passPriorityUntil(Step.BEGIN_COMBAT)
            driver.drain()
            driver.state.projectedState.getController(force) shouldBe opponent
            driver.state.delayedTriggers.isEmpty() shouldBe true
        }
    }
})
