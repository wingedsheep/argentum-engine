package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `Costs.additional.DiscardVariableCards` — "as an additional cost to cast this spell, discard X
 * cards" — and `DynamicAmounts.discardedAsCostTotalManaValue()`, "the total mana value of the
 * discarded cards" (Nahiri's Wrath).
 *
 * The cost defines X (there is no `{X}` in the mana cost), so the caster announces X with the
 * spell (CR 601.2b) — before targets, which is what lets "each of up to X target creatures" be
 * sized by it (601.2c) — and the payment (601.2h) discards exactly X. The damage sums the mana
 * values of every discarded card wherever it now is (the 2016-07-13 Nahiri's Wrath ruling), with
 * X in a discarded card's cost counted as 0 (CR 202.3e).
 */
class DiscardXSpellCostTest : FunSpec({

    val wrath = card("Test Wrath") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        additionalCost(Costs.additional.DiscardVariableCards())
        spell {
            target = TargetObject(
                optional = true,
                filter = TargetFilter(GameObjectFilter.CreatureOrPlaneswalker),
                dynamicMaxCount = DynamicAmounts.xValue(),
            )
            effect = Effects.ForEachTarget(
                Effects.DealDamage(DynamicAmounts.discardedAsCostTotalManaValue(), EffectTarget.ContextTarget(0))
            )
        }
    }
    // Discard fodder with known mana values.
    val fourDrop = card("Test Four Drop") {
        manaCost = "{3}{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(1) }
    }
    val xDrop = card("Test X Drop") {
        manaCost = "{X}{R}{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(DynamicAmounts.xValue()) }
    }

    data class Board(
        val driver: GameTestDriver,
        val caster: EntityId,
        val spell: EntityId,
        val four: EntityId,
        val x: EntityId,
        // Centaur Courser 3/3, Savannah Lions 2/1, Force of Nature 5/5.
        val courser: EntityId,
        val lions: EntityId,
        val force: EntityId,
    )

    fun board(): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(wrath, fourDrop, xDrop))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.putLandOnBattlefield(caster, "Mountain")
        // The opening hand is cleared so the discard pool is exactly the two fodder cards.
        driver.getHand(caster).forEach { driver.moveToGraveyard(it) }
        return Board(
            driver, caster,
            spell = driver.putCardInHand(caster, "Test Wrath"),
            four = driver.putCardInHand(caster, "Test Four Drop"),
            x = driver.putCardInHand(caster, "Test X Drop"),
            courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser"),
            lions = driver.putCreatureOnBattlefield(opponent, "Savannah Lions"),
            force = driver.putCreatureOnBattlefield(opponent, "Force of Nature"),
        )
    }

    fun Board.cast(x: Int?, discarded: List<EntityId>, targets: List<EntityId>) = driver.submit(
        CastSpell(
            playerId = caster,
            cardId = spell,
            targets = targets.map { ChosenTarget.Permanent(it) },
            xValue = x,
            paymentStrategy = PaymentStrategy.AutoPay,
            additionalCostPayment = AdditionalCostPayment(discardedCards = discarded),
        )
    )

    fun Board.resolve() {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()
    }

    fun Board.onBattlefield(id: EntityId) = id in driver.state.getBattlefield()

    test("the cast announces X, capped at the cards that could be discarded, and asks for exactly X") {
        val b = board()
        val action = b.driver.legalActions(b.caster).single {
            (it.action as? CastSpell)?.cardId == b.spell && it.actionType == "CastSpell"
        }
        action.hasXCost shouldBe true
        // Two cards besides the spell itself, which is on the stack as its costs are paid (CR 601.2a).
        action.maxAffordableX shouldBe 2
        action.xConstrainsTargetCount shouldBe true
        val info = action.additionalCostInfo.shouldNotBeNull()
        info.costType shouldBe "DiscardCard"
        info.countIsX shouldBe true
        info.validDiscardTargets shouldContainExactlyInAnyOrder listOf(b.four, b.x)
    }

    test("X = 2 deals the discarded cards' total mana value to each target, X counted as 0") {
        val b = board()
        // Test Four Drop (4) + Test X Drop ({X}{R}{R} off the stack: 2) = 6 to each target.
        val cast = b.cast(x = 2, discarded = listOf(b.four, b.x), targets = listOf(b.courser, b.force))
        withClue("cast: ${cast.error}") { cast.error shouldBe null }
        // The discard is a cost: paid before the spell resolves.
        b.driver.getGraveyard(b.caster).containsAll(listOf(b.four, b.x)) shouldBe true
        b.resolve()
        b.onBattlefield(b.courser) shouldBe false
        withClue("6 damage, not divided, kills the 5/5 too") { b.onBattlefield(b.force) shouldBe false }
        b.onBattlefield(b.lions) shouldBe true
    }

    test("a discarded card with {X} in its cost adds its mana value with X as 0") {
        val b = board()
        // 2 damage: kills the 2/1 ...
        b.cast(x = 1, discarded = listOf(b.x), targets = listOf(b.lions)).error shouldBe null
        b.resolve()
        b.onBattlefield(b.lions) shouldBe false

        // ... but not the 3/3 — X counted as anything but 0 would kill it.
        val b3 = board()
        b3.cast(x = 1, discarded = listOf(b3.x), targets = listOf(b3.courser)).error shouldBe null
        b3.resolve()
        withClue("{X}{R}{R} discarded is worth 2, short of the 3/3's toughness") {
            b3.onBattlefield(b3.courser) shouldBe true
        }
    }

    test("a discarded card still counts after it has left the graveyard") {
        val b = board()
        b.cast(x = 1, discarded = listOf(b.four), targets = listOf(b.courser)).error shouldBe null
        // Move the discarded card from the graveyard to exile while the spell waits on the stack.
        val state = b.driver.state
        b.driver.replaceState(
            state.moveToZone(b.four, ZoneKey(b.caster, Zone.GRAVEYARD), ZoneKey(b.caster, Zone.EXILE))
        )
        b.resolve()
        withClue("4 damage from the exiled discard kills the 3/3") { b.onBattlefield(b.courser) shouldBe false }
    }

    test("X = 0 discards nothing, targets nothing, and resolves") {
        val b = board()
        b.cast(x = 0, discarded = emptyList(), targets = emptyList()).error shouldBe null
        b.resolve()
        listOf(b.courser, b.lions, b.force).all { b.onBattlefield(it) } shouldBe true
        b.driver.getHand(b.caster) shouldContainExactlyInAnyOrder listOf(b.four, b.x)
    }

    test("the discard must match the announced X exactly") {
        val b = board()
        b.cast(x = 2, discarded = listOf(b.four), targets = listOf(b.courser)).error shouldNotBe null
        b.cast(x = 1, discarded = listOf(b.four, b.x), targets = listOf(b.courser)).error shouldNotBe null
        b.cast(x = 2, discarded = listOf(b.four, b.four), targets = listOf(b.courser)).error shouldNotBe null
        b.driver.getHand(b.caster) shouldContainExactlyInAnyOrder listOf(b.spell, b.four, b.x)
    }

    test("no more targets than the announced X") {
        val b = board()
        b.cast(x = 1, discarded = listOf(b.four), targets = listOf(b.courser, b.lions)).error shouldNotBe null
        b.driver.getHand(b.caster) shouldContainExactlyInAnyOrder listOf(b.spell, b.four, b.x)
    }

    test("a cast with no announced X lets the discard define it, as before") {
        val b = board()
        b.cast(x = null, discarded = listOf(b.four), targets = listOf(b.courser)).error shouldBe null
        b.resolve()
        b.onBattlefield(b.courser) shouldBe false
    }
})
