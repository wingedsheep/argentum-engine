package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.MarshalOfZhalfir
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Marshal of Zhalfir (MOM) — {W}{U} Human Knight 2/2.
 * "Other Knights you control get +1/+1." / "{W}{U}, {T}: Tap another target creature."
 */
class MarshalOfZhalfirScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MarshalOfZhalfir))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        return driver
    }

    test("other Knights you control get +1/+1, not the Marshal nor opposing Knights") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val marshal = driver.putCreatureOnBattlefield(me, "Marshal of Zhalfir")
        val myKnight = driver.putCreatureOnBattlefield(me, "First Strike Knight")
        val theirKnight = driver.putCreatureOnBattlefield(opponent, "First Strike Knight")

        val projected = driver.state.projectedState
        projected.getPower(marshal) shouldBe 2
        projected.getToughness(marshal) shouldBe 2
        projected.getPower(myKnight) shouldBe 4
        projected.getToughness(myKnight) shouldBe 2
        projected.getPower(theirKnight) shouldBe 3
        projected.getToughness(theirKnight) shouldBe 1
    }

    test("{W}{U}, {T} taps another target creature; the Marshal can't target itself") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val marshal = driver.putCreatureOnBattlefield(me, "Marshal of Zhalfir")
        driver.removeSummoningSickness(marshal)
        val victim = driver.putCreatureOnBattlefield(opponent, "First Strike Knight")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val abilityId = MarshalOfZhalfir.activatedAbilities.single().id

        driver.giveMana(me, Color.WHITE, 1)
        driver.giveMana(me, Color.BLUE, 1)
        val selfTarget = driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = marshal,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(marshal))
            )
        )
        selfTarget.error shouldNotBe null

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = marshal,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(victim))
            )
        )
        driver.isTapped(marshal) shouldBe true
        driver.bothPass()

        driver.isTapped(victim) shouldBe true
    }
})
