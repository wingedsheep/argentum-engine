package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.CaptainOfTheMists
import com.wingedsheep.mtg.sets.definitions.avr.cards.CathedralSanctifier
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CaptainOfTheMistsScenarioTest : FunSpec({
    test("another Human entering untaps the Captain") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CaptainOfTheMists)
        driver.registerCard(CathedralSanctifier)
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Plains" to 20), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val captainId = driver.putPermanentOnBattlefield(you, "Captain of the Mists")
        driver.tapPermanent(captainId)
        driver.isTapped(captainId) shouldBe true

        val human = driver.putCardInHand(you, "Cathedral Sanctifier")
        driver.giveMana(you, Color.WHITE, 1)
        driver.castSpell(you, human)
        driver.bothPass() // resolve Human
        driver.bothPass() // resolve one ETB trigger
        driver.bothPass() // resolve the other ETB trigger

        driver.isTapped(captainId) shouldBe false
        driver.findPermanent(you, "Captain of the Mists") shouldNotBe null
        driver.findPermanent(you, "Cathedral Sanctifier") shouldNotBe null
    }

    test("{1}{U}, {T}: you may tap or untap target permanent") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CaptainOfTheMists)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opp = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val captainId = driver.putPermanentOnBattlefield(you, "Captain of the Mists")
        driver.removeSummoningSickness(captainId)
        val bears = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")
        driver.isTapped(bears) shouldBe false

        driver.giveMana(you, Color.BLUE, 1)
        driver.giveColorlessMana(you, 1)
        driver.submitSuccess(
            ActivateAbility(
                playerId = you,
                sourceId = captainId,
                abilityId = CaptainOfTheMists.activatedAbilities.first().id,
                targets = listOf(ChosenTarget.Permanent(bears)),
            ),
        )
        driver.bothPass() // resolve the ability — pauses on the may-question

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(you, true)

        val modeDecision = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val tapIndex = modeDecision.options.indexOfFirst { it.startsWith("Tap") }
        (tapIndex >= 0) shouldBe true
        driver.submitDecision(you, OptionChosenResponse(modeDecision.id, tapIndex))

        driver.isTapped(bears) shouldBe true
        withClue("{T} is part of the cost, so the Captain is tapped too") {
            driver.isTapped(captainId) shouldBe true
        }
    }
})
