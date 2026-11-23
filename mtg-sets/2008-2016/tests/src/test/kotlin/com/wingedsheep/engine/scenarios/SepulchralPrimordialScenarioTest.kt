package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.SepulchralPrimordial
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sepulchral Primordial — "When this creature enters, for each opponent, you may put up to one
 * target creature card from that player's graveyard onto the battlefield under your control."
 */
class SepulchralPrimordialScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SepulchralPrimordial))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castPrimordial(driver: GameTestDriver, me: EntityId) {
        val primordial = driver.putCardInHand(me, "Sepulchral Primordial")
        driver.giveMana(me, Color.BLACK, 7)
        driver.castSpell(me, primordial).error.shouldBeNull()
        driver.bothPass()
    }

    test("puts the targeted creature card from the opponent's graveyard onto the battlefield under your control") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val bears = driver.putCardInGraveyard(opponent, "Grizzly Bears")
        val myBears = driver.putCardInGraveyard(me, "Grizzly Bears")

        castPrimordial(driver, me)

        val targets = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        // Only opponents' graveyards are legal; your own creature card is not a target.
        targets.legalTargets.values.flatten() shouldNotContain myBears
        driver.submitTargetSelection(me, listOf(bears)).error.shouldBeNull()
        driver.bothPass()

        val put = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(me, put.options).error.shouldBeNull()

        driver.getPermanents(me) shouldContain bears
        driver.getController(bears) shouldBe me
        driver.getGraveyard(me) shouldContain myBears
    }

    test("you may decline at resolution, leaving the card in its owner's graveyard") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val bears = driver.putCardInGraveyard(opponent, "Grizzly Bears")

        castPrimordial(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, listOf(bears)).error.shouldBeNull()
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(me, emptyList()).error.shouldBeNull()

        driver.getGraveyard(opponent) shouldContain bears
        driver.getPermanents(me) shouldNotContain bears
    }
})
