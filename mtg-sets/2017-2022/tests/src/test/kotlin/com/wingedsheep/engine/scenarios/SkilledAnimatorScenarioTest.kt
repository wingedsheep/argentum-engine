package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m19.cards.SkilledAnimator
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Skilled Animator (M19 #73) — {2}{U} 1/3 Human Artificer.
 *
 * "When this creature enters, target artifact you control becomes an artifact creature with base
 *  power and toughness 5/5 for as long as this creature remains on the battlefield."
 */
class SkilledAnimatorScenarioTest : FunSpec({

    val Trinket = card("Animator Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    val Bauble = card("Animator Bauble") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    data class Setup(
        val driver: GameTestDriver,
        val me: EntityId,
        val trinket: EntityId,
        val bauble: EntityId,
    )

    fun setup(): Setup {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SkilledAnimator, Trinket, Bauble))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val trinket = driver.putPermanentOnBattlefield(me, "Animator Trinket")
        val bauble = driver.putPermanentOnBattlefield(me, "Animator Bauble")
        val animator = driver.putCardInHand(me, "Skilled Animator")
        driver.giveMana(me, Color.BLUE, 3)
        driver.castSpell(me, animator)
        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        return Setup(driver, me, trinket, bauble)
    }

    test("the targeted artifact becomes a 5/5 artifact creature; the other artifact is untouched") {
        val (driver, me, trinket, bauble) = setup()
        driver.submitTargetSelection(me, listOf(trinket))
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.isCreature(trinket) shouldBe true
        projected.hasType(trinket, "ARTIFACT") shouldBe true
        projected.getPower(trinket) shouldBe 5
        projected.getToughness(trinket) shouldBe 5
        projected.isCreature(bauble) shouldBe false
    }

    test("the animation ends when Skilled Animator leaves the battlefield") {
        val (driver, me, trinket, _) = setup()
        driver.submitTargetSelection(me, listOf(trinket))
        driver.bothPass()
        driver.state.projectedState.isCreature(trinket) shouldBe true

        driver.moveToGraveyard(driver.findPermanent(me, "Skilled Animator")!!)

        val projected = driver.state.projectedState
        projected.isCreature(trinket) shouldBe false
        projected.hasType(trinket, "ARTIFACT") shouldBe true
    }

    test("if Skilled Animator is gone before the trigger resolves, the artifact never animates") {
        val (driver, me, trinket, _) = setup()
        driver.submitTargetSelection(me, listOf(trinket))

        driver.moveToGraveyard(driver.findPermanent(me, "Skilled Animator")!!)
        driver.bothPass()

        driver.state.projectedState.isCreature(trinket) shouldBe false
    }
})
