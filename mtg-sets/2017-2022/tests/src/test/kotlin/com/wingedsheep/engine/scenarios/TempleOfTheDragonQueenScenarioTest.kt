package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.BlackDragon
import com.wingedsheep.mtg.sets.definitions.afr.cards.TempleOfTheDragonQueen
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Temple of the Dragon Queen — the Dragon reveal / "you control a Dragon" tapped gate and the
 * as-enters color choice share one OnEnterRun program, so a played land must prompt for both and
 * its mana ability must read the chosen color.
 */
class TempleOfTheDragonQueenScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TempleOfTheDragonQueen, BlackDragon))
        driver.initMirrorMatch(deck = Deck.of("Island" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.chooseColor(color: Color) {
        val decision = pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        submitDecision(activePlayer!!, ColorChosenResponse(decision.id, color)).outcome shouldBe Outcome.Done
    }

    fun GameTestDriver.tapForMana(temple: EntityId): ManaPoolComponent {
        val player = activePlayer!!
        submit(ActivateAbility(player, temple, TempleOfTheDragonQueen.activatedAbilities.single().id))
            .outcome shouldBe Outcome.Done
        return state.getEntity(player)!!.get<ManaPoolComponent>()!!
    }

    test("revealing a Dragon card: enters untapped, then taps for the chosen color") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val dragon = driver.putCardInHand(player, "Black Dragon")
        val temple = driver.putCardInHand(player, "Temple of the Dragon Queen")

        (driver.playLand(player, temple).outcome is Outcome.Paused) shouldBe true
        val reveal = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        reveal.options shouldBe listOf(dragon)
        (driver.submitCardSelection(player, listOf(dragon)).outcome is Outcome.Paused) shouldBe true
        driver.chooseColor(Color.RED)

        driver.isTapped(temple) shouldBe false
        driver.tapForMana(temple).getAmount(Color.RED) shouldBe 1
    }

    test("controlling a Dragon: no reveal prompt, enters untapped, still chooses a color") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCreatureOnBattlefield(player, "Black Dragon")
        driver.putCardInHand(player, "Black Dragon")
        val temple = driver.putCardInHand(player, "Temple of the Dragon Queen")

        (driver.playLand(player, temple).outcome is Outcome.Paused) shouldBe true
        driver.chooseColor(Color.GREEN)

        driver.isTapped(temple) shouldBe false
        driver.tapForMana(temple).getAmount(Color.GREEN) shouldBe 1
    }

    test("declining the reveal with no Dragon in play: enters tapped with the chosen color") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCardInHand(player, "Black Dragon")
        val temple = driver.putCardInHand(player, "Temple of the Dragon Queen")

        (driver.playLand(player, temple).outcome is Outcome.Paused) shouldBe true
        (driver.submitCardSelection(player, emptyList()).outcome is Outcome.Paused) shouldBe true
        driver.chooseColor(Color.WHITE)

        driver.isTapped(temple) shouldBe true
        driver.untapPermanent(temple)
        driver.tapForMana(temple).getAmount(Color.WHITE) shouldBe 1
    }

    test("no Dragon anywhere: enters tapped after the color choice") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val temple = driver.putCardInHand(player, "Temple of the Dragon Queen")

        // The reveal is asked even with no Dragon in hand (#3340), offering nothing to pick.
        (driver.playLand(player, temple).outcome is Outcome.Paused) shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options shouldBe emptyList()
        (driver.submitCardSelection(player, emptyList()).outcome is Outcome.Paused) shouldBe true
        driver.chooseColor(Color.BLACK)

        driver.isTapped(temple) shouldBe true
    }
})
