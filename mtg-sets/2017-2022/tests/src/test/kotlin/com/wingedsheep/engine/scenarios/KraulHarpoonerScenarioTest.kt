package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.grn.cards.CitywatchSphinx
import com.wingedsheep.mtg.sets.definitions.grn.cards.KraulHarpooner
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Kraul Harpooner (GRN) — Undergrowth ETB: up to one target creature you don't control with flying;
 * this gets +X/+0 (X = creature cards in your graveyard), then you may have it fight that creature.
 *
 * The pump must land before the fight (so the fight uses the pumped power), it must happen even
 * with no target, and the may-fight is only offered when a target was chosen.
 */
class KraulHarpoonerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KraulHarpooner, CitywatchSphinx))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver
    }

    /** Cast the Harpooner and resolve everything, answering the target prompt and the may-fight. */
    fun castAndResolve(
        driver: GameTestDriver,
        player: EntityId,
        targets: List<EntityId>,
        fight: Boolean
    ): Int {
        val harpooner = driver.putCardInHand(player, "Kraul Harpooner")
        driver.giveMana(player, Color.GREEN, 2)
        driver.submit(CastSpell(player, harpooner, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        var yesNoPrompts = 0
        var guard = 0
        while (guard++ < 20 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            when (driver.pendingDecision) {
                is ChooseTargetsDecision -> driver.submitTargetSelection(player, targets)
                is YesNoDecision -> { yesNoPrompts++; driver.submitYesNo(player, fight) }
                else -> driver.bothPass()
            }
        }
        return yesNoPrompts
    }

    test("the pump lands before the fight, so a pumped Harpooner kills a 3/4 flier") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCardInGraveyard(player, "Grizzly Bears")
        val sphinx = driver.putCreatureOnBattlefield(opponent, "Citywatch Sphinx")

        castAndResolve(driver, player, listOf(sphinx), fight = true) shouldBe 1

        // 3 + 1 = 4 damage kills the 3/4 Sphinx; the Sphinx's 3 kills the 4/2 Harpooner.
        driver.getGraveyard(opponent) shouldContain sphinx
        driver.findPermanent(player, "Kraul Harpooner") shouldBe null
    }

    test("declining the fight keeps the pump and deals no damage") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCardInGraveyard(player, "Grizzly Bears")
        driver.putCardInGraveyard(player, "Savannah Lions")
        val sphinx = driver.putCreatureOnBattlefield(opponent, "Citywatch Sphinx")

        castAndResolve(driver, player, listOf(sphinx), fight = false) shouldBe 1

        val harpooner = driver.findPermanent(player, "Kraul Harpooner").shouldNotBeNull()
        driver.state.projectedState.getPower(harpooner) shouldBe 5
        driver.findPermanent(opponent, "Citywatch Sphinx") shouldBe sphinx
    }

    test("with no target chosen it still gets +X/+0 and no fight is offered") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCardInGraveyard(player, "Grizzly Bears")
        driver.putCardInGraveyard(player, "Forest") // not a creature card — doesn't count

        castAndResolve(driver, player, emptyList(), fight = true) shouldBe 0

        val harpooner = driver.findPermanent(player, "Kraul Harpooner").shouldNotBeNull()
        driver.state.projectedState.getPower(harpooner) shouldBe 4
        driver.state.projectedState.getToughness(harpooner) shouldBe 2
    }
})
