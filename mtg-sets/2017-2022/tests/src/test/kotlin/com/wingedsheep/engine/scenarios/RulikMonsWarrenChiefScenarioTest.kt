package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dmu.cards.RulikMonsWarrenChief
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rulik Mons, Warren Chief — "Whenever Rulik Mons attacks, look at the top card of your library.
 * If it's a land card, you may put it onto the battlefield tapped. If you didn't put a card onto
 * the battlefield this way, create a 1/1 red Goblin creature token."
 *
 * The payoff is an `IfYouDo` scored by `SuccessCriterion.Auto` over a pausing pipeline, so the
 * branch taken depends on how the gate evaluates after the land choice resumes — exercised here
 * for all three outcomes: land taken, land declined, nonland on top.
 */
class RulikMonsWarrenChiefScenarioTest : FunSpec({

    fun goblinTokens(driver: GameTestDriver, controller: EntityId): Int =
        driver.state.getBattlefield().count { id ->
            val entity = driver.state.getEntity(id)
            entity?.get<CardComponent>()?.name == "Goblin Token" &&
                entity.get<ControllerComponent>()?.playerId == controller
        }

    fun attackingWith(topCard: String): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + RulikMonsWarrenChief)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val rulik = driver.putCreatureOnBattlefield(you, "Rulik Mons, Warren Chief")
        driver.removeSummoningSickness(rulik)
        val top = driver.putCardOnTopOfLibrary(you, topCard)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submitSuccess(DeclareAttackers(you, mapOf(rulik to driver.getOpponent(you))))
        driver.bothPass()
        return driver to top
    }

    test("putting the top land onto the battlefield tapped makes no Goblin") {
        val (driver, top) = attackingWith("Forest")
        val you = driver.activePlayer!!
        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.maxSelections shouldBe 1
        driver.submitCardSelection(you, listOf(top)).error shouldBe null

        driver.state.getBattlefield().contains(top) shouldBe true
        driver.isTapped(top) shouldBe true
        goblinTokens(driver, you) shouldBe 0
    }

    test("declining the land leaves it on top and creates a Goblin") {
        val (driver, top) = attackingWith("Forest")
        val you = driver.activePlayer!!
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(you, emptyList()).error shouldBe null

        driver.state.getLibrary(you).first() shouldBe top
        goblinTokens(driver, you) shouldBe 1
    }

    test("a nonland top card stays on top and creates a Goblin") {
        val (driver, top) = attackingWith("Grizzly Bears")
        val you = driver.activePlayer!!
        if (driver.pendingDecision is SelectCardsDecision) {
            driver.submitCardSelection(you, emptyList()).error shouldBe null
        }

        driver.state.getLibrary(you).first() shouldBe top
        goblinTokens(driver, you) shouldBe 1
    }
})
