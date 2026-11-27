package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.mtg.sets.definitions.dka.cards.BurdenOfGuilt
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BurdenOfGuiltScenarioTest : FunSpec({

    test("{1}: taps the enchanted creature") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + DarkAscensionSet.cards)
            initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val aura = driver.putCardInHand(you, "Burden of Guilt")
        driver.giveMana(you, Color.WHITE, 1)
        driver.castSpell(you, aura, listOf(bears)).outcome shouldBe Outcome.Done
        driver.bothPass()

        val burden = driver.findPermanent(you, "Burden of Guilt")!!
        driver.isTapped(bears) shouldBe false

        driver.giveColorlessMana(you, 1)
        driver.submitSuccess(
            ActivateAbility(
                playerId = you,
                sourceId = burden,
                abilityId = BurdenOfGuilt.activatedAbilities.first().id,
            )
        )
        driver.bothPass()

        driver.isTapped(bears) shouldBe true
    }
})
