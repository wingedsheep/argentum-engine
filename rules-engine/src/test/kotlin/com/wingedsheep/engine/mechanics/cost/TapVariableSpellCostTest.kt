package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `Costs.additional.TapPermanentsVariable` — "as an additional cost to cast this spell, tap any
 * number of untapped creatures you control" — read back by `DynamicAmounts.permanentsTappedAsCost()`
 * ("the number of creatures tapped this way").
 *
 * The creatures are tapped as the cost is paid (CR 601.2h), so only untapped ones may be chosen
 * (CR 701.26a), and summoning sickness doesn't matter: it restricts the {T} symbol, not a tap paid
 * as a cost (CR 302.6). Zero is a legal choice. The count is fixed at payment — a creature tapped
 * this way that leaves the battlefield before resolution still counts.
 */
class TapVariableSpellCostTest : FunSpec({

    val pyre = card("Test Pyre") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        additionalCost(Costs.additional.TapPermanentsVariable())
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(DynamicAmounts.permanentsTappedAsCost() * 3, t)
        }
    }

    data class Board(
        val driver: GameTestDriver,
        val caster: EntityId,
        val opponent: EntityId,
        val spell: EntityId,
        val mine: List<EntityId>,
        val theirs: EntityId,
    )

    fun board(mine: Int): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(pyre))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.putLandOnBattlefield(caster, "Mountain")
        val spell = driver.putCardInHand(caster, "Test Pyre")
        return Board(
            driver, caster, opponent, spell,
            List(mine) { driver.putCreatureOnBattlefield(caster, "Grizzly Bears") },
            driver.putCreatureOnBattlefield(opponent, "Hill Giant"),
        )
    }

    fun Board.cast(tapped: List<EntityId>?) = driver.submit(
        CastSpell(
            playerId = caster,
            cardId = spell,
            targets = listOf(ChosenTarget.Player(opponent)),
            paymentStrategy = PaymentStrategy.AutoPay,
            additionalCostPayment = tapped?.let { AdditionalCostPayment(variableCostPermanents = it) },
        )
    )

    fun Board.resolve() {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
    }

    test("the cast offers a tap picker over your untapped creatures, from none up") {
        val b = board(mine = 3)
        b.driver.tapPermanent(b.mine[2])
        val action = b.driver.legalActions(b.caster).single {
            (it.action as? CastSpell)?.cardId == b.spell && it.actionType == "CastSpell"
        }
        val info = action.additionalCostInfo.shouldNotBeNull()
        info.costType shouldBe "TapVariable"
        info.tapCount shouldBe 0
        // The tapped creature, the opponent's creature and the land are not candidates.
        info.validTapTargets shouldContainExactlyInAnyOrder b.mine.take(2)
    }

    test("tapping two creatures taps them as the spell is cast and deals six") {
        val b = board(mine = 3)
        b.cast(b.mine.take(2)).error shouldBe null
        // A cost: paid before the spell resolves — and summoning-sick creatures may pay it.
        b.mine.take(2).all { b.driver.isTapped(it) } shouldBe true
        b.driver.isTapped(b.mine[2]) shouldBe false
        b.resolve()
        b.driver.getLifeTotal(b.opponent) shouldBe 14
    }

    test("tapping none is a legal payment and deals no damage") {
        val b = board(mine = 2)
        b.cast(emptyList()).error shouldBe null
        b.resolve()
        b.driver.getLifeTotal(b.opponent) shouldBe 20
        b.mine.none { b.driver.isTapped(it) } shouldBe true
    }

    test("an already-tapped, an opponent's, or a twice-chosen creature can't pay") {
        val b = board(mine = 2)
        b.driver.tapPermanent(b.mine[1])
        b.cast(listOf(b.mine[1])).error shouldNotBe null
        b.cast(listOf(b.theirs)).error shouldNotBe null
        b.cast(listOf(b.mine[0], b.mine[0])).error shouldNotBe null
        b.driver.isTapped(b.mine[0]) shouldBe false
        b.driver.isTapped(b.theirs) shouldBe false
    }

    test("a creature tapped this way that leaves before resolution still counts") {
        val b = board(mine = 2)
        b.cast(b.mine).error shouldBe null
        b.driver.moveToGraveyard(b.mine[0])
        b.resolve()
        b.driver.getLifeTotal(b.opponent) shouldBe 14
    }
})
