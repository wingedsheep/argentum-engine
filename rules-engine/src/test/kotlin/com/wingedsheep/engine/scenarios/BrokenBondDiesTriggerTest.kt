package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dominaria.cards.BrokenBond
import com.wingedsheep.mtg.sets.definitions.dominaria.cards.SparringConstruct
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Regression: a triggered ability firing from events emitted *before* a spell's
 * resolution pauses for a decision was lost. Concretely, Broken Bond destroys
 * Sparring Construct and *then* asks "may put a land from your hand onto the
 * battlefield". Sparring Construct's "when this creature dies, put a +1/+1
 * counter on target creature you control" trigger must still fire after the
 * put-land decision resolves.
 */
class BrokenBondDiesTriggerTest : FunSpec({

    val allCards = TestCards.all + listOf(BrokenBond, SparringConstruct)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    test("Sparring Construct's dies trigger fires after Broken Bond pauses for 'may put a land'") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val activePlayer = driver.activePlayer!!
        val opponent = if (activePlayer == driver.player1) driver.player2 else driver.player1

        // Sparring Construct on our battlefield (the Broken Bond target).
        val construct = driver.putCreatureOnBattlefield(activePlayer, "Sparring Construct")
        // Another creature we control to receive the +1/+1 counter.
        val recipient = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Broken Bond in hand + a land in hand so the "may put a land" step pauses.
        val brokenBond = driver.putCardInHand(activePlayer, "Broken Bond")
        driver.putCardInHand(activePlayer, "Forest")

        // Pay {1}{G}
        driver.giveColorlessMana(activePlayer, 1)
        driver.giveMana(activePlayer, com.wingedsheep.sdk.core.Color.GREEN, 1)

        val cast = driver.castSpell(activePlayer, brokenBond, targets = listOf(construct))
        (cast.error == null) shouldBe true

        // Both pass → Broken Bond resolves → destroys Sparring Construct → pauses for
        // the "may put a land from your hand" decision.
        driver.bothPass()

        val putLandDecision = driver.pendingDecision
        putLandDecision shouldNotBe null
        (putLandDecision is SelectCardsDecision) shouldBe true

        // Skip putting a land (submit empty selection).
        driver.submitCardSelection(activePlayer, emptyList())

        // The Dies trigger should now be on the stack asking us to target a creature we control.
        val targetDecision = driver.pendingDecision
        targetDecision shouldNotBe null
        (targetDecision is ChooseTargetsDecision) shouldBe true

        driver.submitTargetSelection(activePlayer, listOf(recipient))
        // Resolve the Dies trigger.
        driver.bothPass()

        val counters = driver.state.getEntity(recipient)?.get<CountersComponent>()
        val plusOne = counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        plusOne shouldBe 1
    }
})
