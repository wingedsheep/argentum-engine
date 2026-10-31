package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.MaximumHandSize
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.FolioOfFancies
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Folio of Fancies — "Players have no maximum hand size. {X}{X}, {T}: Each player draws X cards.
 * {2}{U}, {T}: Each opponent mills cards equal to the number of cards in their hand."
 */
class FolioOfFanciesScenarioTest : FunSpec({
    val drawAbility = FolioOfFancies.activatedAbilities[0].id
    val millAbility = FolioOfFancies.activatedAbilities[1].id

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(FolioOfFancies)
        initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.max(playerId: EntityId): Int? = MaximumHandSize.effective(
        state, playerId, cardRegistry, services.conditionEvaluator, services.dynamicAmountEvaluator
    )

    test("every player, not just its controller, has no maximum hand size") {
        val d = driver(); val you = d.activePlayer!!; val opp = d.getOpponent(you)
        d.max(opp) shouldBe 7
        d.putPermanentOnBattlefield(you, "Folio of Fancies")
        d.max(you) shouldBe null
        d.max(opp) shouldBe null
    }

    test("an opponent with more than seven cards discards nothing at their own cleanup") {
        val d = driver(); val you = d.activePlayer!!; val opp = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Folio of Fancies")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        repeat(10) { d.putCardInHand(opp, "Island") }
        val before = d.getHandSize(opp)
        d.passPriorityUntil(Step.UPKEEP, you)
        withClue("the opponent kept their whole hand through their cleanup") {
            d.getHandSize(opp) shouldBe before
        }
    }

    test("{X}{X}, {T}: each player draws X cards") {
        val d = driver(); val you = d.activePlayer!!; val opp = d.getOpponent(you)
        val folio = d.putPermanentOnBattlefield(you, "Folio of Fancies")
        val yours = d.getHandSize(you); val theirs = d.getHandSize(opp)
        d.giveColorlessMana(you, 4)
        d.submitSuccess(ActivateAbility(you, folio, drawAbility, xValue = 2))
        d.bothPass()
        d.getHandSize(you) shouldBe yours + 2
        d.getHandSize(opp) shouldBe theirs + 2
    }

    test("{2}{U}, {T}: each opponent mills cards equal to their own hand size") {
        val d = driver(); val you = d.activePlayer!!; val opp = d.getOpponent(you)
        val folio = d.putPermanentOnBattlefield(you, "Folio of Fancies")
        repeat(3) { d.putCardInHand(you, "Island") }
        val theirHand = d.getHandSize(opp)
        val theirLibrary = d.state.getLibrary(opp).size
        val yourLibrary = d.state.getLibrary(you).size
        d.giveMana(you, Color.BLUE, 1)
        d.giveColorlessMana(you, 2)
        d.submitSuccess(ActivateAbility(you, folio, millAbility))
        d.bothPass()
        withClue("the opponent milled their own hand size, not yours") {
            d.getGraveyard(opp).size shouldBe theirHand
            d.state.getLibrary(opp).size shouldBe theirLibrary - theirHand
        }
        d.state.getLibrary(you).size shouldBe yourLibrary
    }
})
