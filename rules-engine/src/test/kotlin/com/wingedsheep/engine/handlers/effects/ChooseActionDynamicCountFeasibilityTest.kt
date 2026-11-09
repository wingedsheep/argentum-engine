package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [FeasibilityCheck.ControlsPermanentMatching.dynamicCount] on a
 * [com.wingedsheep.sdk.scripting.effects.ChooseActionEffect] option: "lose N life unless you
 * sacrifice N permanents" (Archfiend of Spite) offers the sacrifice only while the chooser controls
 * at least N permanents, where N is evaluated when the question is asked — and a fixed `count` is
 * ignored once a dynamic one is given.
 */
class ChooseActionDynamicCountFeasibilityTest : FunSpec({

    fun punisher(name: String, needed: Int, fixedCount: Int = 1) = card(name) {
        manaCost = "{U}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.ChooseAction(
                choices = listOf(
                    EffectChoice(
                        label = "Sacrifice $needed permanents",
                        effect = Effects.Sacrifice(
                            GameObjectFilter.Any, DynamicAmount.Fixed(needed), EffectTarget.Controller
                        ),
                        feasibilityCheck = FeasibilityCheck.ControlsPermanentMatching(
                            GameObjectFilter.Any,
                            count = fixedCount,
                            dynamicCount = DynamicAmount.Fixed(needed)
                        )
                    ),
                    EffectChoice(
                        label = "Lose $needed life",
                        effect = Effects.LoseLife(needed, EffectTarget.Controller)
                    )
                )
            )
        }
    }

    val threeOrLife = punisher("Tithe of Three", needed = 3)
    // fixed count 1 would be satisfied by a lone permanent; the dynamic 3 must win.
    val dynamicBeatsFixed = punisher("Tithe Beats Fixed", needed = 3, fixedCount = 1)

    fun driver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(threeOrLife, dynamicBeatsFixed))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.cast(name: String) {
        val me = activePlayer!!
        val spell = putCardInHand(me, name)
        giveMana(me, Color.BLUE, 1)
        castSpell(me, spell)
        bothPass()
    }

    test("with fewer permanents than the dynamic count, only the life loss is feasible and nothing is asked") {
        val driver = driver()
        val me = driver.activePlayer!!
        repeat(2) { driver.putPermanentOnBattlefield(me, "Forest") }

        driver.cast("Tithe of Three")

        driver.state.pendingDecision shouldBe null
        driver.getLifeTotal(me) shouldBe 17
        driver.getPermanents(me).size shouldBe 2
    }

    test("a fixed count that would be met is ignored once a dynamic count is given") {
        val driver = driver()
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, "Forest")

        driver.cast("Tithe Beats Fixed")

        driver.state.pendingDecision shouldBe null
        driver.getLifeTotal(me) shouldBe 17
        driver.getPermanents(me).size shouldBe 1
    }

    test("with exactly the dynamic count, both options are offered and sacrificing takes exactly that many") {
        val driver = driver()
        val me = driver.activePlayer!!
        val forests = (1..3).map { driver.putPermanentOnBattlefield(me, "Forest") }
        val keeper = driver.putPermanentOnBattlefield(me, "Forest")

        driver.cast("Tithe of Three")

        val decision = driver.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.options shouldBe listOf("Sacrifice 3 permanents", "Lose 3 life")
        driver.submitDecision(me, OptionChosenResponse(decision.id, 0))

        val pick = driver.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.minSelections shouldBe 3
        pick.maxSelections shouldBe 3
        driver.submitCardSelection(me, forests)

        driver.getLifeTotal(me) shouldBe 20
        driver.getPermanents(me) shouldBe listOf(keeper)
    }

    test("choosing the life loss with enough permanents available keeps every permanent") {
        val driver = driver()
        val me = driver.activePlayer!!
        repeat(4) { driver.putPermanentOnBattlefield(me, "Forest") }

        driver.cast("Tithe of Three")

        val decision = driver.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        driver.submitDecision(me, OptionChosenResponse(decision.id, 1))

        driver.getLifeTotal(me) shouldBe 17
        driver.getPermanents(me).size shouldBe 4
    }
})
