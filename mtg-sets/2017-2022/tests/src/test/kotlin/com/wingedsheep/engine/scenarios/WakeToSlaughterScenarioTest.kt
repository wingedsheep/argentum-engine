package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.WakeToSlaughter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Wake to Slaughter: an opponent picks which of up to two graveyard creature cards goes to your
 * hand; the other returns to the battlefield with haste and is exiled at the next end step.
 */
class WakeToSlaughterScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WakeToSlaughter)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castWake(active: EntityId, targets: List<EntityId>) {
        val spell = putCardInHand(active, "Wake to Slaughter")
        giveMana(active, Color.BLACK, 1)
        giveMana(active, Color.RED, 1)
        giveColorlessMana(active, 3)
        castSpellWithTargets(active, spell, targets.map { ChosenTarget.Card(it, active, Zone.GRAVEYARD) })
        bothPass()
    }

    test("the opponent's pick goes to hand; the other returns with haste and is exiled at end step") {
        val driver = setup()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        val bears = driver.putCardInGraveyard(active, "Grizzly Bears")
        val courser = driver.putCardInGraveyard(active, "Centaur Courser")

        driver.castWake(active, listOf(bears, courser))

        val pick = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.playerId shouldBe opponent
        pick.options.toSet() shouldBe setOf(bears, courser)
        driver.submitCardSelection(opponent, listOf(bears))

        driver.getHand(active) shouldContain bears
        driver.findPermanent(active, "Centaur Courser") shouldBe courser
        driver.state.projectedState.hasKeyword(courser, Keyword.HASTE) shouldBe true

        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        driver.findPermanent(active, "Centaur Courser") shouldBe null
        driver.getExile(active) shouldContain courser
    }

    test("a single target must go to hand") {
        val driver = setup()
        val active = driver.activePlayer!!
        val bears = driver.putCardInGraveyard(active, "Grizzly Bears")

        driver.castWake(active, listOf(bears))

        driver.isPaused shouldBe false
        driver.getHand(active) shouldContain bears
        driver.findPermanent(active, "Grizzly Bears") shouldBe null
        driver.getGraveyard(active) shouldNotContain bears
    }
})
