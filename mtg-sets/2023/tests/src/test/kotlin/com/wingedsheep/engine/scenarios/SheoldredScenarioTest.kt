package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.Sheoldred
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sheoldred // The True Scriptures (MOM #125).
 */
class SheoldredScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PredefinedTokens.allTokens + listOf(Sheoldred))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun nameOf(driver: GameTestDriver, id: EntityId): String =
        driver.state.getEntity(id)!!.get<CardComponent>()!!.name

    /** Answer target prompts with every legal target (up to the max). */
    fun answerTargets(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 12 && driver.isPaused) {
            when (val decision = driver.pendingDecision) {
                is ChooseTargetsDecision -> {
                    val chosen = decision.targetRequirements.associate { req ->
                        req.index to decision.legalTargets[req.index].orEmpty().take(req.maxTargets)
                    }
                    driver.submitDecision(decision.playerId, TargetsResponse(decision.id, chosen))
                }
                else -> driver.autoResolveDecision()
            }
        }
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && (driver.state.stack.isNotEmpty() || driver.isPaused)) {
            answerTargets(driver)
            if (driver.state.stack.isNotEmpty()) driver.bothPass()
        }
    }

    fun advanceToNextTurnMain(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        resolveStack(driver)
    }

    fun activateTransform(driver: GameTestDriver, you: EntityId, sheoldred: EntityId): Outcome {
        driver.giveMana(you, Color.BLACK, 1)
        driver.giveColorlessMana(you, 4)
        return driver.submit(
            ActivateAbility(playerId = you, sourceId = sheoldred, abilityId = Sheoldred.activatedAbilities.first().id)
        ).outcome
    }

    test("entering makes each opponent sacrifice a nontoken creature or planeswalker") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        val oppBear = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

        val card = driver.putCardInHand(you, "Sheoldred")
        driver.giveMana(you, Color.BLACK, 2)
        driver.giveColorlessMana(you, 3)
        driver.castSpell(you, card).outcome shouldBe Outcome.Done
        resolveStack(driver)

        driver.findPermanent(you, "Sheoldred") shouldNotBe null
        driver.state.getBattlefield().contains(oppBear) shouldBe false
        driver.getGraveyardCardNames(opp).contains("Grizzly Bears") shouldBe true
    }

    test("transforming needs an opponent with eight or more cards in their graveyard") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        val sheoldred = driver.putCreatureOnBattlefield(you, "Sheoldred")
        repeat(7) { driver.putCardInGraveyard(opp, "Swamp") }
        repeat(8) { driver.putCardInGraveyard(you, "Swamp") } // your own graveyard doesn't count

        activateTransform(driver, you, sheoldred) shouldNotBe Outcome.Done

        driver.putCardInGraveyard(opp, "Swamp")
        activateTransform(driver, you, sheoldred) shouldBe Outcome.Done
        resolveStack(driver)
        nameOf(driver, sheoldred) shouldBe "The True Scriptures"
    }

    test("the Saga: destroy per opponent, discard three and mill three, then reanimate everything and flip back") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        val sheoldred = driver.putCreatureOnBattlefield(you, "Sheoldred")
        val oppBear = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val yourBear = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        repeat(8) { driver.putCardInGraveyard(opp, "Swamp") }

        // Chapter I: up to one target creature or planeswalker the opponent controls.
        activateTransform(driver, you, sheoldred) shouldBe Outcome.Done
        resolveStack(driver)
        nameOf(driver, sheoldred) shouldBe "The True Scriptures"
        driver.state.getEntity(sheoldred)!!.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 1
        driver.state.getBattlefield().contains(oppBear) shouldBe false
        driver.state.getBattlefield().contains(yourBear) shouldBe true

        // Chapter II: the opponent discards three cards, then mills three.
        advanceToNextTurnMain(driver) // opponent's turn
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 300) // your upkeep, before lore 2
        val handBefore = driver.getHandSize(opp)
        val graveyardBefore = driver.getGraveyard(opp).size
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        resolveStack(driver)
        driver.state.getEntity(sheoldred)!!.get<CountersComponent>()!!.getCount(CounterType.LORE) shouldBe 2
        driver.getHandSize(opp) shouldBe handBefore - 3
        driver.getGraveyard(opp).size shouldBe graveyardBefore + 6

        // Chapter III: every creature card in every graveyard comes back under your control.
        driver.putCardInGraveyard(you, "Hill Giant")
        advanceToNextTurnMain(driver) // opponent's turn
        advanceToNextTurnMain(driver) // your turn: lore 3

        nameOf(driver, sheoldred) shouldBe "Sheoldred"
        driver.state.getBattlefield(you).contains(sheoldred) shouldBe true
        driver.findPermanent(you, "Hill Giant") shouldNotBe null
        val reanimated = driver.state.getBattlefield(you).filter { nameOf(driver, it) == "Grizzly Bears" }
        reanimated.size shouldBe 2 // your surviving bear plus the opponent's destroyed one
        driver.getGraveyardCardNames(opp).contains("Grizzly Bears") shouldBe false
    }
})
