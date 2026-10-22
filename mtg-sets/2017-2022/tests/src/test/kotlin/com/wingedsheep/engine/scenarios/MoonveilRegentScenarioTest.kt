package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.MoonveilRegent
import com.wingedsheep.mtg.sets.definitions.rav.cards.LightningHelix
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class MoonveilRegentScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MoonveilRegent, LightningHelix))
        driver.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("discarding the hand draws a card for each of the cast spell's colors") {
        val driver = setup()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.putCreatureOnBattlefield(player, "Moonveil Regent")

        val helix = driver.putCardInHand(player, "Lightning Helix")
        val heldCards = driver.getHand(player) - helix
        driver.giveMana(player, Color.RED, 1)
        driver.giveMana(player, Color.WHITE, 1)
        driver.castSpell(player, helix, targets = listOf(opponent)).outcome shouldBe Outcome.Done
        driver.bothPass() // Moonveil Regent's cast trigger

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(player, true)

        driver.getGraveyard(player) shouldContainAll heldCards
        val newHand = driver.getHand(player)
        newHand.size shouldBe 2
        newHand shouldNotContainAnyOf heldCards
    }

    test("declining keeps the hand and draws nothing") {
        val driver = setup()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.putCreatureOnBattlefield(player, "Moonveil Regent")

        val helix = driver.putCardInHand(player, "Lightning Helix")
        val heldCards = driver.getHand(player) - helix
        driver.giveMana(player, Color.RED, 1)
        driver.giveMana(player, Color.WHITE, 1)
        driver.castSpell(player, helix, targets = listOf(opponent)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.submitYesNo(player, false)

        driver.getHand(player) shouldContainExactlyInAnyOrder heldCards
    }

    test("the death trigger counts colors among permanents you control as it resolves, without the Regent") {
        val driver = setup()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val regent = driver.putCreatureOnBattlefield(player, "Moonveil Regent")
        driver.putCreatureOnBattlefield(player, "Centaur Courser") // green
        driver.putCreatureOnBattlefield(player, "Savannah Lions") // white

        val doomBlade = driver.putCardInHand(player, "Doom Blade")
        driver.giveMana(player, Color.BLACK, 2)
        driver.castSpell(player, doomBlade, targets = listOf(regent)).outcome shouldBe Outcome.Done
        driver.bothPass() // cast trigger
        driver.submitYesNo(player, false)
        driver.bothPass() // Doom Blade resolves; the Regent dies

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(player, listOf(opponent))
        driver.bothPass() // death trigger resolves

        // Green and white remain; the Regent's red left with it.
        driver.getLifeTotal(opponent) shouldBe 18
    }
})
