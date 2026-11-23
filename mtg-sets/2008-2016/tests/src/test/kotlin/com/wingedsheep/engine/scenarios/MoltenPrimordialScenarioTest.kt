package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.MoltenPrimordial
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Molten Primordial — "When this creature enters, for each opponent, gain control of up to one
 * target creature that player controls until end of turn. Untap those creatures. They gain haste
 * until end of turn."
 */
class MoltenPrimordialScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MoltenPrimordial))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castPrimordial(driver: GameTestDriver, me: EntityId) {
        val primordial = driver.putCardInHand(me, "Molten Primordial")
        driver.giveMana(me, Color.RED, 7)
        driver.castSpell(me, primordial).error.shouldBeNull()
        driver.bothPass()
    }

    test("steals the opponent's tapped creature until end of turn, untapped and hasty") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.tapPermanent(bears)

        castPrimordial(driver, me)

        val targets = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        targets.targetRequirements.single().minTargets shouldBe 0
        targets.targetRequirements.single().maxTargets shouldBe 1
        driver.submitTargetSelection(me, listOf(bears)).error.shouldBeNull()
        driver.bothPass()

        driver.state.projectedState.getController(bears) shouldBe me
        driver.isTapped(bears) shouldBe false
        driver.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true

        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.projectedState.getController(bears) shouldBe opponent
    }

    test("choosing no target steals nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        castPrimordial(driver, me)

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, emptyList()).error.shouldBeNull()
        driver.bothPass()

        driver.state.projectedState.getController(bears) shouldBe opponent
    }
})
