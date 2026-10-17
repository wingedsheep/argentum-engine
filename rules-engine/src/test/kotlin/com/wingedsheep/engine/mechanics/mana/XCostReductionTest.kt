package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Generic cost reductions pay the announced X (CR 601.2f): the total cost is the mana cost with
 * X's value in place of {X}, plus increases, minus reductions. So a reduction bigger than a spell's
 * printed generic keeps paying into the X, and only the coloured part is beyond its reach — both
 * at payment (`CastCostTotaller`) and in the X ceiling the enumerator offers.
 */
class XCostReductionTest : FunSpec({

    val discount = card("X Discount Totem") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Spells you cast cost {1} less to cast."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any),
                modification = CostModification.ReduceGenericBy(CostReductionSource.Fixed(1)),
            )
        }
    }

    val tax = card("X Tax Totem") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Spells cost {1} more to cast."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any),
                modification = CostModification.IncreaseGeneric(1),
            )
        }
    }

    val bolt = card("Green X Bolt") {
        manaCost = "{X}{G}"
        typeLine = "Instant"
        oracleText = "Green X Bolt deals X damage to target player."
        spell { effect = Effects.DealDamage(DynamicAmounts.xValue(), target(Targets.Player)) }
    }

    fun newGame(discounts: Int, taxes: Int = 0, forests: Int = 1): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(discount, tax, bolt))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        repeat(discounts) { driver.putPermanentOnBattlefield(me, "X Discount Totem") }
        repeat(taxes) { driver.putPermanentOnBattlefield(me, "X Tax Totem") }
        repeat(forests) { driver.putLandOnBattlefield(me, "Forest") }
        return driver to me
    }

    fun GameTestDriver.castBolt(me: EntityId, x: Int) =
        castXSpell(me, putCardInHand(me, "Green X Bolt"), x, listOf(getOpponent(me)))

    test("a reduction past the printed generic pays the X: {X}{G}, X = 2, two discounts → {G}") {
        val (driver, me) = newGame(discounts = 2)
        driver.castBolt(me, 2).error shouldBe null
        driver.bothPass()
        driver.getLifeTotal(driver.getOpponent(me)) shouldBe 18
    }

    test("the reduction never reaches the coloured part: X = 3 with two discounts needs {1}{G}") {
        val (driver, me) = newGame(discounts = 2)
        driver.castBolt(me, 3).error shouldNotBe null
    }

    test("increases apply before reductions, against the X-inclusive total") {
        // {X}{G} with X = 2, +{1}, −{2} → {1}{G}: two Forests pay it, one doesn't.
        val (one, me1) = newGame(discounts = 2, taxes = 1, forests = 1)
        one.castBolt(me1, 2).error shouldNotBe null
        val (two, me2) = newGame(discounts = 2, taxes = 1, forests = 2)
        two.castBolt(me2, 2).error shouldBe null
    }

    test("the offered X ceiling counts the reduction the printed generic couldn't absorb") {
        val (driver, me) = newGame(discounts = 2)
        val boltId = driver.putCardInHand(me, "Green X Bolt")
        val offer = driver.legalActions(me)
            .firstOrNull { (it.action as? CastSpell)?.cardId == boltId }
            .shouldNotBeNull()
        offer.maxAffordableX shouldBe 2
    }
})
