package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ArtfulDodgeScenarioTest : FunSpec({

    fun newDriver() = GameTestDriver().apply {
        registerCards(TestCards.all + DarkAscensionSet.cards)
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("cast from hand, target creature can't be blocked this turn") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        val dodge = driver.putCardInHand(you, "Artful Dodge")
        driver.giveMana(you, Color.BLUE, 1)
        driver.castSpell(you, dodge, listOf(bears)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
        driver.getGraveyard(you).contains(dodge) shouldBe true
    }

    test("flashback {U} casts it from the graveyard and exiles it") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(you, "Grizzly Bears")

        val dodge = driver.putCardInGraveyard(you, "Artful Dodge")
        driver.giveMana(you, Color.BLUE, 1)
        driver.submit(
            CastSpell(
                playerId = you,
                cardId = dodge,
                targets = listOf(ChosenTarget.Permanent(bears)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.FLASHBACK,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
        driver.getExile(you).contains(dodge) shouldBe true
        driver.getGraveyard(you).contains(dodge) shouldBe false
    }
})
