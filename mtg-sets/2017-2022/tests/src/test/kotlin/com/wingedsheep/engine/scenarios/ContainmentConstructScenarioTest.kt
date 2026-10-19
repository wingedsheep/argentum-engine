package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.ContainmentConstruct
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize

/**
 * Containment Construct (NEO #243) — "Whenever you discard a card, you may exile that card from
 * your graveyard. If you do, you may play that card this turn."
 *
 * Covers the accept path (exiled, castable from exile this turn, not next turn) and the decline
 * path (the card stays in the graveyard with no permission).
 */
class ContainmentConstructScenarioTest : FunSpec({

    val testBear = card("Construct Test Bear") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val discardSpell = card("Construct Test Rummage") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "Discard a card."
        spell {
            effect = Effects.Discard(1)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ContainmentConstruct)
        driver.registerCard(testBear)
        driver.registerCard(discardSpell)
        driver.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castActionsFor(playerId: EntityId, cardId: EntityId): List<CastSpell> =
        LegalActionEnumerator.create(cardRegistry).enumerate(state, playerId)
            .mapNotNull { it.action as? CastSpell }.filter { it.cardId == cardId }

    /** Resolve the stack, discarding [discard] and answering the Construct's prompt with [exile]. */
    fun GameTestDriver.settle(discard: EntityId, exile: Boolean) {
        repeat(40) {
            val decision = pendingDecision
            when {
                decision is SelectCardsDecision -> submitCardSelection(decision.playerId, listOf(discard))
                decision is YesNoDecision -> submitYesNo(decision.playerId, exile)
                decision != null -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> return
            }
        }
    }

    /** Discard a bear with the Construct on the battlefield; returns the discarded card. */
    fun GameTestDriver.discardWithConstruct(exile: Boolean): EntityId {
        val me = player1
        putCreatureOnBattlefield(me, "Containment Construct")
        val bear = putCardInHand(me, "Construct Test Bear")
        val rummage = putCardInHand(me, "Construct Test Rummage")
        giveMana(me, Color.BLACK, 1)
        castSpell(me, rummage)
        settle(discard = bear, exile = exile)
        return bear
    }

    test("accepting exiles the discarded card and lets you cast it from exile this turn") {
        val driver = newDriver()
        val me = driver.player1

        val bear = driver.discardWithConstruct(exile = true)

        driver.state.getZone(me, Zone.GRAVEYARD).contains(bear).shouldBeFalse()
        driver.state.getZone(me, Zone.EXILE).contains(bear).shouldBeTrue()

        driver.giveMana(me, Color.BLACK, 2)
        driver.castActionsFor(me, bear) shouldHaveSize 1
        driver.castSpell(me, bear)
        driver.bothPass()
        driver.state.getBattlefield().contains(bear).shouldBeTrue()
    }

    test("the permission ends with the turn") {
        val driver = newDriver()
        val me = driver.player1

        val bear = driver.discardWithConstruct(exile = true)

        driver.passPriorityUntil(Step.UPKEEP, activePlayer = driver.player2)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = me)
        driver.state.getZone(me, Zone.EXILE).contains(bear).shouldBeTrue()
        driver.giveMana(me, Color.BLACK, 2)
        driver.castActionsFor(me, bear).shouldBeEmpty()
    }

    test("declining leaves the card in the graveyard") {
        val driver = newDriver()
        val me = driver.player1

        val bear = driver.discardWithConstruct(exile = false)

        driver.state.getZone(me, Zone.GRAVEYARD).contains(bear).shouldBeTrue()
        driver.state.getZone(me, Zone.EXILE).contains(bear).shouldBeFalse()
        driver.castActionsFor(me, bear).shouldBeEmpty()
    }
})
