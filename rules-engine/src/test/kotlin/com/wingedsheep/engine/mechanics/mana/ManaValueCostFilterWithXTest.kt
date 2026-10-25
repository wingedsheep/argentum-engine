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
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * A cost modifier whose spell filter reads mana value ("spells with mana value 4 or greater cost
 * {1} less") judges an X spell by the X it is cast with: the spell is on the stack while its cost
 * is determined, and there the X counts at its chosen value (CR 202.3e). Both the payment
 * (`CastCostTotaller`) and the X ceiling the enumerator offers follow it, in either direction —
 * a discount the X reaches, and one the X grows out of.
 */
class ManaValueCostFilterWithXTest : FunSpec({

    val bigDiscount = card("Big Spell Totem") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Spells you cast with mana value 4 or greater cost {1} less to cast."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any.manaValueAtLeast(4)),
                modification = CostModification.ReduceGeneric(1),
            )
        }
    }

    val smallDiscount = card("Small Spell Totem") {
        manaCost = "{2}"
        typeLine = "Artifact"
        oracleText = "Spells you cast with mana value 2 or less cost {1} less to cast."
        staticAbility {
            ability = ModifySpellCost(
                target = SpellCostTarget.YouCast(GameObjectFilter.Any.manaValueAtMost(2)),
                modification = CostModification.ReduceGeneric(1),
            )
        }
    }

    val bolt = card("Green X Bolt") {
        manaCost = "{X}{G}"
        typeLine = "Instant"
        oracleText = "Green X Bolt deals X damage to target player."
        spell { effect = Effects.DealDamage(DynamicAmounts.xValue(), target(Targets.Player)) }
    }

    fun newGame(totem: String, forests: Int): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(bigDiscount, smallDiscount, bolt))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        driver.putPermanentOnBattlefield(me, totem)
        repeat(forests) { driver.putLandOnBattlefield(me, "Forest") }
        return driver to me
    }

    fun GameTestDriver.castBolt(me: EntityId, x: Int) =
        castXSpell(me, putCardInHand(me, "Green X Bolt"), x, listOf(getOpponent(me)))

    fun GameTestDriver.offeredMaxX(me: EntityId): Int? {
        val boltId = putCardInHand(me, "Green X Bolt")
        return legalActions(me).firstOrNull { (it.action as? CastSpell)?.cardId == boltId }
            .shouldNotBeNull().maxAffordableX
    }

    test("an X that lifts the mana value to the threshold earns the discount: X = 3 → {2}{G}") {
        val (driver, me) = newGame("Big Spell Totem", forests = 3)
        driver.castBolt(me, 3).error shouldBe null
        driver.bothPass()
        driver.getLifeTotal(driver.getOpponent(me)) shouldBe 17
    }

    test("below the threshold the spell pays full price: X = 2 is {2}{G}") {
        val (driver, me) = newGame("Big Spell Totem", forests = 2)
        driver.castBolt(me, 2).error shouldNotBe null
    }

    test("the offered X ceiling reaches the discounted X") {
        val (driver, me) = newGame("Big Spell Totem", forests = 3)
        driver.offeredMaxX(me) shouldBe 3
    }

    test("an X that grows the spell out of a mana-value-or-less discount loses it") {
        // {X}{G}: X = 1 is mana value 2 → {G}; X = 2 is mana value 3 → full {2}{G}.
        val (cheap, me1) = newGame("Small Spell Totem", forests = 1)
        cheap.castBolt(me1, 1).error shouldBe null
        val (dear, me2) = newGame("Small Spell Totem", forests = 2)
        dear.castBolt(me2, 2).error shouldNotBe null
    }

    test("the offered X ceiling stops where the discount ends") {
        // Two Forests: X = 1 costs {G}, X = 2 costs {2}{G} — the X-blind price would offer 2.
        val (driver, me) = newGame("Small Spell Totem", forests = 2)
        driver.offeredMaxX(me) shouldBe 1
    }
})
