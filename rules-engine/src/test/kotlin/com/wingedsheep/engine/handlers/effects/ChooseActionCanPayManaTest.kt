package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [FeasibilityCheck.CanPayMana] on a [com.wingedsheep.sdk.scripting.effects.ChooseActionEffect]
 * option: "you may pay {G} or {W}. If you do, …" offers only the colours the player can pay right
 * now, and asks nothing when no colour can be paid.
 */
class ChooseActionCanPayManaTest : FunSpec({

    val payForLife = card("Colour Tithe") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.ChooseAction(
                choices = listOf("{G}", "{W}").map { symbol ->
                    EffectChoice(
                        label = "Pay $symbol",
                        effect = Effects.PayMana(symbol) then Effects.GainLife(3, EffectTarget.Controller),
                        feasibilityCheck = FeasibilityCheck.CanPayMana(ManaCost.parse(symbol))
                    )
                } + EffectChoice(label = "Don't pay", effect = Effects.Nothing)
            )
        }
    }

    fun driver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(payForLife))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("with no source of either colour, only 'don't pay' is feasible and nothing is asked") {
        val driver = driver()
        val me = driver.activePlayer!!
        val spell = driver.putCardInHand(me, "Colour Tithe")
        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpell(me, spell)
        driver.bothPass()

        driver.state.pendingDecision shouldBe null
        driver.getLifeTotal(me) shouldBe 20
    }

    test("only the payable colour is offered, and paying it taps the source and runs the payoff") {
        val driver = driver()
        val me = driver.activePlayer!!
        val forest = driver.putPermanentOnBattlefield(me, "Forest")
        val spell = driver.putCardInHand(me, "Colour Tithe")
        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpell(me, spell)
        driver.bothPass()

        val decision = driver.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.options shouldBe listOf("Pay {G}", "Don't pay")
        driver.submitDecision(me, OptionChosenResponse(decision.id, 0))

        driver.isTapped(forest) shouldBe true
        driver.getLifeTotal(me) shouldBe 23
    }

    test("declining leaves the source untapped and skips the payoff") {
        val driver = driver()
        val me = driver.activePlayer!!
        val forest = driver.putPermanentOnBattlefield(me, "Forest")
        val spell = driver.putCardInHand(me, "Colour Tithe")
        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpell(me, spell)
        driver.bothPass()

        val decision = driver.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submitDecision(me, OptionChosenResponse(decision.id, 1))

        driver.isTapped(forest) shouldBe false
        driver.getLifeTotal(me) shouldBe 20
    }
})
