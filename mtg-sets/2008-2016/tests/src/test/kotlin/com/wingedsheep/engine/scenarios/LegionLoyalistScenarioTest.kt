package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.LegionLoyalist
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Legion Loyalist — Battalion grants the whole team first strike, trample and
 * "can't be blocked by creature tokens" this turn.
 *
 * The token restriction is the interesting part: it's a *granted* `CantBeBlockedBy` static fanned out
 * over every creature you control, so the test declares a token block on a non-Loyalist attacker and
 * expects the engine to reject it, while a nontoken creature still blocks. The control case attacks
 * with only two creatures, so Battalion doesn't trigger and the token block is legal.
 */
class LegionLoyalistScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(LegionLoyalist)
        return driver
    }

    test("battalion: the team gains first strike and trample and can't be blocked by creature tokens") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val loyalist = driver.putCreatureOnBattlefield(me, "Legion Loyalist")
        val courserA = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val courserB = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        driver.removeSummoningSickness(courserA)
        driver.removeSummoningSickness(courserB)
        val tokenBlocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.addComponent(tokenBlocker, TokenComponent)
        val cardBlocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(loyalist, courserA, courserB), opponent).outcome shouldBe Outcome.Done
        driver.bothPass() // resolve the Battalion trigger

        withClue("every creature you control gains first strike and trample") {
            listOf(loyalist, courserA, courserB).forEach { id ->
                driver.state.projectedState.hasKeyword(id, Keyword.FIRST_STRIKE) shouldBe true
                driver.state.projectedState.hasKeyword(id, Keyword.TRAMPLE) shouldBe true
            }
        }

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        withClue("a creature token can't block a Courser") {
            driver.declareBlockers(opponent, mapOf(tokenBlocker to listOf(courserA))).outcome shouldNotBe Outcome.Done
        }
        withClue("a nontoken creature still blocks fine") {
            driver.declareBlockers(opponent, mapOf(cardBlocker to listOf(courserA))).outcome shouldBe Outcome.Done
        }
    }

    test("without battalion a creature token blocks normally") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val loyalist = driver.putCreatureOnBattlefield(me, "Legion Loyalist")
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        driver.removeSummoningSickness(courser)
        val tokenBlocker = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.addComponent(tokenBlocker, TokenComponent)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(loyalist, courser), opponent).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        driver.state.projectedState.hasKeyword(courser, Keyword.FIRST_STRIKE) shouldBe false
        withClue("control: only two attackers, so the token block is legal") {
            driver.declareBlockers(opponent, mapOf(tokenBlocker to listOf(courser))).outcome shouldBe Outcome.Done
        }
    }
})
