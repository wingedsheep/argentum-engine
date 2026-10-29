package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `Costs.additional.SacrificeX` — "as an additional cost to cast this spell, sacrifice X creatures"
 * (`CostAtom.VariablePermanents.exactly = XValue`). There is no `{X}` in the mana cost; the cost
 * defines X. The caster announces X with the spell (CR 601.2b), chooses exactly X targets for
 * "Destroy X target creatures" (601.2c), and then sacrifices exactly X creatures (601.2h). X may be
 * 0. Because targets are chosen before the cost is paid, a creature the caster sacrifices may also
 * have been targeted; it is simply gone (an illegal target) at resolution.
 */
class SacrificeXSpellCostTest : FunSpec({

    val culling = card("Test Culling") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        additionalCost(Costs.additional.SacrificeX(GameObjectFilter.Creature))
        spell {
            targets(TargetFilter.Creature, exactly = DynamicAmounts.xValue())
            effect = Effects.ForEachTarget(Effects.Destroy(EffectTarget.ContextTarget(0)))
        }
    }

    data class Board(
        val driver: GameTestDriver,
        val caster: EntityId,
        val spell: EntityId,
        val mine: List<EntityId>,
        val theirs: List<EntityId>,
    )

    fun board(mine: Int, theirs: Int): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(culling))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.putLandOnBattlefield(caster, "Swamp")
        val spell = driver.putCardInHand(caster, "Test Culling")
        return Board(
            driver, caster, spell,
            List(mine) { driver.putCreatureOnBattlefield(caster, "Grizzly Bears") },
            List(theirs) { driver.putCreatureOnBattlefield(opponent, "Hill Giant") },
        )
    }

    fun Board.cast(x: Int?, sacrificed: List<EntityId>, targets: List<EntityId>) = driver.submit(
        CastSpell(
            playerId = caster,
            cardId = spell,
            targets = targets.map { ChosenTarget.Permanent(it) },
            xValue = x,
            paymentStrategy = PaymentStrategy.AutoPay,
            additionalCostPayment = AdditionalCostPayment(variableCostPermanents = sacrificed),
        )
    )

    fun Board.resolve() {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
    }

    fun Board.onBattlefield(id: EntityId) = id in driver.state.getBattlefield()

    test("the cast announces X, capped at the creatures that could be sacrificed, and asks for exactly X sacrifices") {
        val b = board(mine = 2, theirs = 3)
        val action = b.driver.legalActions(b.caster).single {
            (it.action as? CastSpell)?.cardId == b.spell && it.actionType == "CastSpell"
        }
        action.hasXCost shouldBe true
        // Two creatures to sacrifice; the five creatures on the battlefield are all legal targets.
        action.maxAffordableX shouldBe 2
        action.xConstrainsTargetCountExactly shouldBe true
        val info = action.additionalCostInfo.shouldNotBeNull()
        info.costType shouldBe "SacrificeVariable"
        info.sacrificeCountIsX shouldBe true
        info.validSacrificeTargets shouldContainExactlyInAnyOrder b.mine
    }

    test("too few creatures to sacrifice caps X even when there are more targets") {
        val b = board(mine = 1, theirs = 4)
        b.driver.legalActions(b.caster).single {
            (it.action as? CastSpell)?.cardId == b.spell && it.actionType == "CastSpell"
        }.maxAffordableX shouldBe 1
    }

    test("X = 0 sacrifices nothing, targets nothing, and resolves") {
        val b = board(mine = 2, theirs = 2)
        b.cast(x = 0, sacrificed = emptyList(), targets = emptyList()).error shouldBe null
        b.resolve()
        (b.mine + b.theirs).all { b.onBattlefield(it) } shouldBe true
        b.driver.state.stack.isEmpty() shouldBe true
    }

    test("X = 2 sacrifices exactly two and destroys exactly two targets") {
        val b = board(mine = 3, theirs = 3)
        b.cast(x = 2, sacrificed = b.mine.take(2), targets = b.theirs.take(2)).error shouldBe null
        // The sacrifice is a cost: paid before the spell resolves.
        b.mine.take(2).none { b.onBattlefield(it) } shouldBe true
        b.onBattlefield(b.mine[2]) shouldBe true
        b.resolve()
        b.theirs.take(2).none { b.onBattlefield(it) } shouldBe true
        b.onBattlefield(b.theirs[2]) shouldBe true
        b.onBattlefield(b.mine[2]) shouldBe true
    }

    test("the sacrifice must match the announced X exactly") {
        val b = board(mine = 3, theirs = 3)
        b.cast(x = 2, sacrificed = b.mine.take(1), targets = b.theirs.take(2)).error shouldNotBe null
        b.cast(x = 2, sacrificed = b.mine, targets = b.theirs.take(2)).error shouldNotBe null
        b.cast(x = 2, sacrificed = listOf(b.mine[0], b.mine[0]), targets = b.theirs.take(2)).error shouldNotBe null
        // An opponent's creature can't pay.
        b.cast(x = 1, sacrificed = b.theirs.take(1), targets = b.theirs.drop(1).take(1)).error shouldNotBe null
        // No announced X reads as 0, so any sacrifice is too many.
        b.cast(x = null, sacrificed = b.mine.take(1), targets = emptyList()).error shouldNotBe null
        b.mine.all { b.onBattlefield(it) } shouldBe true
    }

    test("the targets must match the announced X exactly") {
        val b = board(mine = 3, theirs = 3)
        b.cast(x = 2, sacrificed = b.mine.take(2), targets = b.theirs.take(1)).error shouldNotBe null
        b.cast(x = 1, sacrificed = b.mine.take(1), targets = b.theirs.take(2)).error shouldNotBe null
        b.mine.all { b.onBattlefield(it) } shouldBe true
    }

    test("an X larger than the creatures you can sacrifice can't be paid") {
        val b = board(mine = 1, theirs = 3)
        b.cast(x = 2, sacrificed = b.mine, targets = b.theirs.take(2)).error shouldNotBe null
        b.onBattlefield(b.mine.single()) shouldBe true
    }

    test("a creature targeted and then sacrificed is gone at resolution; the other target is still destroyed") {
        // Targets are chosen (CR 601.2c) before costs are paid (601.2h), so the caster may target
        // a creature they then sacrifice. It's an illegal target on resolution; the rest resolve.
        val b = board(mine = 2, theirs = 1)
        b.cast(x = 2, sacrificed = b.mine, targets = listOf(b.mine[0], b.theirs[0])).error shouldBe null
        b.resolve()
        b.onBattlefield(b.theirs[0]) shouldBe false
        b.mine.none { b.onBattlefield(it) } shouldBe true
    }
})
