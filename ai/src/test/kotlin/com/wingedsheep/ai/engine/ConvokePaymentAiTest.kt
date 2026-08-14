package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.mtg.sets.definitions.ecl.cards.LoftyDreams
import com.wingedsheep.mtg.sets.definitions.ecl.cards.UnexpectedAssistance
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ConvokePaymentAiTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(UnexpectedAssistance, LoftyDreams))
        initMirrorMatch(Deck.of("Island" to 20))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.addConvokeResources() {
        val player = activePlayer!!
        repeat(2) { putLandOnBattlefield(player, "Island") }
        repeat(3) { putCreatureOnBattlefield(player, "Grizzly Bears") }
    }

    test("AI materializes Convoke payment for Unexpected Assistance") {
        val driver = driver()
        val player = driver.activePlayer!!
        driver.addConvokeResources()
        val spell = driver.putCardInHand(player, "Unexpected Assistance")
        val legal = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, player)
            .single { (it.action as? CastSpell)?.cardId == spell }

        val chosen = AIPlayer.create(driver.cardRegistry, player).chooseFrom(driver.state, listOf(legal)).action
            as CastSpell
        chosen.alternativePayment?.convokedCreatures?.size shouldBe 3
        driver.submit(chosen).isSuccess shouldBe true
    }

    test("AI preserves targets while materializing Convoke payment for Lofty Dreams") {
        val driver = driver()
        val player = driver.activePlayer!!
        driver.addConvokeResources()
        val auraTarget = driver.putCreatureOnBattlefield(player, "Force of Nature")
        val spell = driver.putCardInHand(player, "Lofty Dreams")
        val legal = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, player)
            .single { (it.action as? CastSpell)?.cardId == spell }

        val chosen = AIPlayer.create(driver.cardRegistry, player).chooseFrom(driver.state, listOf(legal)).action
            as CastSpell
        chosen.alternativePayment?.convokedCreatures?.size shouldBe 3
        (chosen.targets.single() as ChosenTarget.Permanent).entityId shouldBe auraTarget
        driver.submit(chosen).isSuccess shouldBe true
    }
})
