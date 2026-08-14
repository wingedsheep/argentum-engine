package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.PlayWithAdditionalCostComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.InsideInformation
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Inside Information — opponent-library exile and its life-for-mana play permission. */
class InsideInformationScenarioTest : FunSpec({

    fun createDriver(startingLife: Int = 20): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + InsideInformation)
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = startingLife,
            startingPlayer = 0,
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (state.stack.isNotEmpty() && state.pendingDecision == null && guard++ < 30) bothPass()
    }

    fun GameTestDriver.castInsideInformation(
        caster: EntityId,
        opponent: EntityId,
        xValue: Int,
    ) {
        giveMana(caster, Color.BLACK, 2)
        giveColorlessMana(caster, xValue)
        val card = putCardInHand(caster, "Inside Information")
        castXSpell(caster, card, xValue, targets = listOf(opponent)).error shouldBe null
        resolveAll()
    }

    test("exiles the top X cards and lets the caster play lands or cast spells for life this turn") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        val forest = driver.putCardOnTopOfLibrary(opponent, "Forest")
        val hillGiant = driver.putCardOnTopOfLibrary(opponent, "Hill Giant")

        driver.castInsideInformation(caster, opponent, xValue = 2)

        driver.getExile(opponent).take(2).toSet() shouldBe setOf(forest, hillGiant)
        val actions = driver.legalActions(caster)
        actions.any { it.actionType == "PlayLand" && (it.action as? PlayLand)?.cardId == forest } shouldBe true
        val castAction = actions.single { (it.action as? CastSpell)?.cardId == hillGiant }
        castAction.affordable shouldBe true
        castAction.manaCostString shouldBe "{0}"
        castAction.additionalCostInfo?.description shouldBe "Pay 4 life"

        val lifeBefore = driver.getLifeTotal(caster)
        driver.submit(
            CastSpell(
                playerId = caster,
                cardId = hillGiant,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        driver.resolveAll()

        driver.getLifeTotal(caster) shouldBe lifeBefore - 4
        driver.findPermanent(caster, "Hill Giant") shouldBe hillGiant
    }

    test("a spell whose mana value exceeds the caster's life is shown unaffordable and rejected") {
        val driver = createDriver(startingLife = 3)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        val hillGiant = driver.putCardOnTopOfLibrary(opponent, "Hill Giant")

        driver.castInsideInformation(caster, opponent, xValue = 1)

        val castAction = driver.legalActions(caster).single { (it.action as? CastSpell)?.cardId == hillGiant }
        castAction.affordable shouldBe false
        castAction.additionalCostInfo?.description shouldBe "Pay 4 life"
        driver.submit(CastSpell(playerId = caster, cardId = hillGiant)).error shouldBe
            "Not enough life to pay 4 life (its mana value)"
    }

    test("the permission expires at the end of the turn") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        val hillGiant = driver.putCardOnTopOfLibrary(opponent, "Hill Giant")

        driver.castInsideInformation(caster, opponent, xValue = 1)
        driver.legalActions(caster).any { (it.action as? CastSpell)?.cardId == hillGiant } shouldBe true

        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)

        driver.state.activePlayerId shouldBe opponent
        driver.legalActions(caster).any { (it.action as? CastSpell)?.cardId == hillGiant } shouldBe false
        driver.state.getEntity(hillGiant)?.get<PlayWithAdditionalCostComponent>() shouldBe null
    }
})
