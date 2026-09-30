package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [com.wingedsheep.sdk.scripting.effects.LockLifeLossEffect] — "you can't lose life" (CR 119.8):
 * life loss, lowering set-life effects, and life payments are all shut off, while life gain is not.
 */
class LockLifeLossTest : FunSpec({

    fun instant(name: String, script: CardScript) =
        CardDefinition.instant(name = name, manaCost = ManaCost.parse("{W}"), oracleText = name, script = script)

    val lockForever = instant("Lock Loss Forever", CardScript.spell(Effects.LockLifeLoss(EffectTarget.Controller)))
    val lockThisTurn = instant(
        "Lock Loss This Turn",
        CardScript.spell(Effects.LockLifeLoss(EffectTarget.Controller, Duration.EndOfTurn))
    )
    val loseThree = instant("Lose Three", CardScript.spell(Effects.LoseLife(3, EffectTarget.Controller)))
    val burnThree = instant("Burn Three", CardScript.spell(Effects.DealDamage(3, EffectTarget.Controller)))
    val setToFive = instant("Set To Five", CardScript.spell(Effects.SetLifeTotal(5, EffectTarget.Controller)))
    val gainTwo = instant("Gain Two", CardScript.spell(Effects.GainLife(2)))
    val payThree = instant(
        "Pay Three Draw",
        CardScript.spell(Effects.DrawCards(1), additionalCosts = listOf(Costs.additional.PayLife(3)))
    )

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(lockForever, lockThisTurn, loseThree, burnThree, setToFive, gainTwo, payThree))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun cast(driver: GameTestDriver, player: EntityId, name: String) =
        driver.putCardInHand(player, name).let { card ->
            driver.giveMana(player, Color.WHITE, 1)
            driver.castSpell(player, card).also { if (it.error == null) driver.bothPass() }
        }

    test("a locked player loses no life, can't be set lower, and can't pay life — but still gains") {
        val driver = newDriver()
        val me = driver.player1
        cast(driver, me, "Lock Loss Forever").error shouldBe null
        driver.state.isLifeLossLocked(me) shouldBe true

        withClue("lose life") {
            cast(driver, me, "Lose Three").error shouldBe null
            driver.getLifeTotal(me) shouldBe 20
        }
        withClue("damage") {
            cast(driver, me, "Burn Three").error shouldBe null
            driver.getLifeTotal(me) shouldBe 20
        }
        withClue("set life lower") {
            cast(driver, me, "Set To Five").error shouldBe null
            driver.getLifeTotal(me) shouldBe 20
        }
        withClue("pay life as an additional cost (CR 119.8)") {
            driver.state.canPayLife(me, 3) shouldBe false
            driver.state.canPayLife(me, 0) shouldBe true
            cast(driver, me, "Pay Three Draw").error shouldNotBe null
            driver.getLifeTotal(me) shouldBe 20
        }
        withClue("life gain is a separate lock") {
            cast(driver, me, "Gain Two").error shouldBe null
            driver.getLifeTotal(me) shouldBe 22
        }
    }

    test("an end-of-turn lock expires in cleanup") {
        val driver = newDriver()
        val me = driver.player1
        cast(driver, me, "Lock Loss This Turn").error shouldBe null
        cast(driver, me, "Lose Three").error shouldBe null
        driver.getLifeTotal(me) shouldBe 20

        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.isLifeLossLocked(me) shouldBe false
        driver.state.canPayLife(me, 3) shouldBe true
    }
})
