package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * An activated ability whose divided total is its own X ("{X}, {T}: deal X damage divided as you
 * choose among any number of targets") and "… dealt damage this way" read off that division
 * (`DividedDamage(storeDamagedAs = …)`).
 *
 * X is announced before the targets and the division (CR 601.2b precedes 601.2d), so the legal
 * action carries the total for every X it may take (`totalDamageByX`) and the validator checks a
 * division against the announced X. The stored collection holds only what the division actually
 * dealt damage to — players included — on both the announced path and the resolution-time one.
 */
class DividedDamageByXActivationTest : FunSpec({
    val probe = card("Divided X Probe") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
            targets(AnyTarget(count = 1, minCount = 0, optional = true, unlimited = true, dynamicMaxCount = DynamicAmounts.xValue()))
            effect = Effects.Pipeline {
                val damaged = runStoringCollection {
                    Effects.DividedDamage(total = 0, dynamicTotal = DynamicAmounts.xValue(), storeDamagedAs = it)
                }
                run(Effects.ForEachInCollection(damaged, Effects.CantBlock(EffectTarget.IterationEntity)))
            }
        }
    }
    val abilityId = probe.activatedAbilities.single().id

    fun newGame(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + probe)
        d.initMirrorMatch(Deck.of("Mountain" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.damage(id: EntityId): Int = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    fun GameTestDriver.activate(x: Int, targets: List<ChosenTarget>, division: Map<EntityId, Int>?, source: EntityId) =
        submit(ActivateAbility(player1, source, abilityId, targets = targets, xValue = x,
            damageDistribution = division, paymentStrategy = PaymentStrategy.FromPool))

    test("the offer prices the division for every affordable X") {
        val d = newGame()
        d.putPermanentOnBattlefield(d.player1, "Divided X Probe")
        d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.giveMana(d.player1, Color.RED, 3)

        val offer = d.legalActions(d.player1).single { (it.action as? ActivateAbility)?.abilityId == abilityId }
        offer.requiresDamageDistribution shouldBe true
        offer.totalDamageByX shouldBe mapOf(0 to 0, 1 to 1, 2 to 2, 3 to 3)
    }

    test("an announced division is checked against X and stores everything it damaged, players included") {
        val d = newGame()
        val source = d.putPermanentOnBattlefield(d.player1, "Divided X Probe")
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        d.giveMana(d.player1, Color.RED, 3)
        val targets = listOf(ChosenTarget.Permanent(courser), ChosenTarget.Player(d.player2))

        d.activate(3, targets, mapOf(courser to 1, d.player2 to 1), source).error shouldNotBe null

        d.activate(3, targets, mapOf(courser to 2, d.player2 to 1), source).error shouldBe null
        d.bothPass().error shouldBe null

        d.damage(courser) shouldBe 2
        d.getLifeTotal(d.player2) shouldBe 19
        d.state.projectedState.cantBlock(courser) shouldBe true
        d.state.projectedState.cantBlock(lions) shouldBe false
    }

    test("a division made at resolution stores what it damaged too") {
        val d = newGame()
        val source = d.putPermanentOnBattlefield(d.player1, "Divided X Probe")
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val lions = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")
        d.giveMana(d.player1, Color.RED, 2)

        d.activate(2, listOf(ChosenTarget.Permanent(courser), ChosenTarget.Permanent(lions)), null, source).error shouldBe null
        d.bothPass().error shouldBe null
        val decision = d.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        d.submitDecision(d.player1, DistributionResponse(decision.id, mapOf(courser to 1, lions to 1))).error shouldBe null

        d.damage(courser) shouldBe 1
        d.state.projectedState.cantBlock(courser) shouldBe true
    }
})
