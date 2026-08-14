package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.DocOcksHenchmen
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Doc Ock's Henchmen (SPM #30) — {2}{U} Creature — Human Villain 2/1.
 *   Flash
 *   Whenever this creature attacks, it connives.
 *
 * Verifies the attack trigger fires the shared connive pipeline on the Henchmen itself:
 * discarding a nonland grows it with a +1/+1 counter; discarding a land does not.
 */
class DocOcksHenchmenScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DocOcksHenchmen)
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        return driver
    }

    fun attackAndConnive(discardName: String): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2

        val henchmen = driver.putCreatureOnBattlefield(attacker, "Doc Ock's Henchmen")
        driver.removeSummoningSickness(henchmen)

        // The connive draw pulls this card; the pre-placed hand card is what we discard.
        driver.putCardOnTopOfLibrary(attacker, "Grizzly Bears")
        val toDiscard = driver.putCardInHand(attacker, discardName)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(henchmen), defender)

        // Resolve the "whenever this creature attacks" trigger → connive.
        driver.bothPass()

        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        return Triple(driver, henchmen, toDiscard)
    }

    test("attacking connives; discarding a nonland puts a +1/+1 counter on the Henchmen") {
        val (driver, henchmen, nonland) = attackAndConnive("Grizzly Bears")
        val attacker = driver.player1

        val decision = driver.pendingDecision as SelectCardsDecision
        driver.submitDecision(
            attacker,
            CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(nonland))
        )
        driver.isPaused shouldBe false

        driver.state.getGraveyard(attacker).contains(nonland) shouldBe true

        val counters = driver.state.getEntity(henchmen)?.get<CountersComponent>()
        (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 1
    }

    test("attacking connives; discarding a land adds no counter") {
        val (driver, henchmen, land) = attackAndConnive("Forest")
        val attacker = driver.player1

        val decision = driver.pendingDecision as SelectCardsDecision
        driver.submitDecision(
            attacker,
            CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(land))
        )
        driver.isPaused shouldBe false

        driver.state.getGraveyard(attacker).contains(land) shouldBe true

        val counters = driver.state.getEntity(henchmen)?.get<CountersComponent>()
        (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
    }
})
