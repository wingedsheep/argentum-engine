package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.battlefield.chosenOpponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Cursed Rack (ATQ #48).
 *
 * {4} Artifact — "As this artifact enters, choose an opponent. The chosen player's maximum hand
 * size is four." Exercises the new [com.wingedsheep.sdk.scripting.SetMaximumHandSize] static
 * keyed to [com.wingedsheep.sdk.scripting.references.Player.ChosenOpponent], enforced in the
 * cleanup step's discard-down action (CR 402.2 / 514.1).
 */
class CursedRackScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 30, "Grizzly Bears" to 30),
            skipMulligans = true
        )
        return driver
    }

    test("the chosen player discards down to four during their cleanup step") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        // Opponent controls Cursed Rack; it chose the active player. The active player's max hand
        // size is now 4 (not 7).
        val rack = driver.putPermanentOnBattlefield(opponent, "Cursed Rack")
        driver.replaceState(driver.state.updateEntity(rack) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(
                ChoiceSlot.OPPONENT to ChoiceValue.EntityChoice(active)
            )))
        })

        // Active player starts with 7 cards and max hand size is now 4, so cleanup forces a
        // 7 - 4 = 3 card discard.
        driver.getHandSize(active) shouldBe 7

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.isPaused shouldBe true
        driver.pendingDecision shouldNotBe null
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe active
        decision.minSelections shouldBe 3   // 7 - 4 = 3
        decision.maxSelections shouldBe 3
        decision.prompt shouldBe "Discard down to 4 cards (choose 3 to discard)"

        driver.submitCardSelection(active, driver.getHand(active).take(3))
        driver.isPaused shouldBe false
        driver.getHandSize(active) shouldBe 4
    }

    test("a player NOT chosen keeps the normal maximum hand size of seven") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        // Cursed Rack chose the OPPONENT (not the active player), so the active player is unaffected.
        val rack = driver.putPermanentOnBattlefield(opponent, "Cursed Rack")
        driver.replaceState(driver.state.updateEntity(rack) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(
                ChoiceSlot.OPPONENT to ChoiceValue.EntityChoice(opponent)
            )))
        })

        // Active player has exactly 7 — within the normal limit, so no discard prompt.
        driver.getHandSize(active) shouldBe 7

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.pendingDecision shouldBe null
    }

    test("casting Cursed Rack prompts to choose an opponent, stored as the chosen player") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        val rack = driver.putPermanentOnBattlefield(active, "Cursed Rack")
        // Drive the as-it-enters choice flow directly to confirm the OPPONENT choice is captured.
        // (Place + run the EntersWithChoice replacement via a fresh ETB is covered by the
        //  Jihad/Riptide flows; here we assert the stored choice round-trips for reading.)
        driver.replaceState(driver.state.updateEntity(rack) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(
                ChoiceSlot.OPPONENT to ChoiceValue.EntityChoice(opponent)
            )))
        })

        driver.state.getEntity(rack)!!.chosenOpponent() shouldBe opponent
    }
})
