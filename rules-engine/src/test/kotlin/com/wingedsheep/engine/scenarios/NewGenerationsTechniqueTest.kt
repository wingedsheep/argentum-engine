package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * New Generation's Technique (TMT #126) — Sorcery, Sneak {2}{G}. "Search your
 * library for up to two basic land cards, put them onto the battlefield tapped, then shuffle."
 */
class NewGenerationsTechniqueTest : FunSpec({
    test("fetches two basic lands onto the battlefield tapped") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30), startingLife = 20)
        val player = driver.activePlayer!!
        val spell = driver.putCardInHand(player, "New Generation's Technique")
        driver.giveMana(player, Color.GREEN, 4)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val forestsBefore = driver.getPermanents(player).count {
            driver.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Forest"
        }
        driver.castSpell(player, spell).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty() && driver.pendingDecision == null) driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(player, decision.options.take(2))
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val forests = driver.getPermanents(player).filter {
            driver.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Forest"
        }
        forests.size shouldBe forestsBefore + 2
        forests.takeLast(2).all { driver.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
    }
})
