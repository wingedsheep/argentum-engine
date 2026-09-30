package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Replicate (CR 702.56a): "As an additional cost to cast this spell, you may pay [cost] any number
 * of times" and "When you cast this spell, if a replicate cost was paid for it, copy it for each
 * time its replicate cost was paid. If the spell has any targets, you may choose new targets for any
 * of the copies."
 */
class ReplicateTest : FunSpec({

    val manaReplicate = card("Test Replicate Ping") {
        manaCost = "{R}"
        typeLine = "Instant"
        keywordAbility(KeywordAbility.replicate("{1}"))
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }

    val energyReplicate = card("Test Energy Replicate Ping") {
        manaCost = "{R}"
        typeLine = "Instant"
        keywordAbility(KeywordAbility.replicate(Costs.additional.PayPlayerCounters(CounterType.ENERGY, 3)))
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }

    val kickerPing = card("Test Kicker Ping") {
        manaCost = "{R}"
        typeLine = "Instant"
        keywordAbility(KeywordAbility.kicker("{1}"))
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(manaReplicate, energyReplicate, kickerPing))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.giveEnergy(player: EntityId, amount: Int) {
        replaceState(state.updateEntity(player) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.ENERGY, amount))
        })
    }

    fun GameTestDriver.energy(player: EntityId): Int =
        state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    fun GameTestDriver.cast(card: EntityId, target: EntityId, times: Int, slot: ChoiceSlot? = ChoiceSlot.REPLICATED) =
        submit(
            CastSpell(
                playerId = activePlayer!!,
                cardId = card,
                targets = listOf(ChosenTarget.Player(target)),
                paymentStrategy = PaymentStrategy.AutoPay,
                declaredCostSlot = slot,
                declaredCostTimes = times,
            )
        )

    /** Resolve everything, keeping each copy's target when asked to choose new ones. */
    fun GameTestDriver.resolveAll(keepTarget: EntityId): Int {
        var retargetPrompts = 0
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 40) {
            val decision = state.pendingDecision
            if (decision is ChooseTargetsDecision) {
                retargetPrompts++
                submitTargetSelection(decision.playerId, listOf(keepTarget)).error shouldBe null
            } else {
                bothPass()
            }
        }
        return retargetPrompts
    }

    test("paying replicate twice charges the cost twice and copies the spell twice") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        repeat(3) { driver.putLandOnBattlefield(caster, "Mountain") }
        val ping = driver.putCardInHand(caster, "Test Replicate Ping")

        driver.cast(ping, opponent, times = 2).outcome shouldBe Outcome.Done

        // {R} + {1}{1}: every land was tapped for the cost.
        driver.getPermanents(caster).all { driver.isTapped(it) } shouldBe true
        // One replicate trigger above the spell (CR 702.56a — one trigger, N copies).
        val trigger = driver.state.stack.last()
        driver.state.getEntity(trigger)?.get<TriggeredAbilityOnStackComponent>() shouldNotBe null

        val prompts = driver.resolveAll(opponent)
        prompts shouldBe 2
        driver.getLifeTotal(opponent) shouldBe 17
    }

    test("the copies are separate spells, each allowed a new target") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        repeat(2) { driver.putLandOnBattlefield(caster, "Mountain") }
        val ping = driver.putCardInHand(caster, "Test Replicate Ping")

        driver.cast(ping, opponent, times = 1).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard++ < 10) driver.bothPass()
        val decision = driver.state.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(decision.playerId, listOf(caster)).outcome shouldBe Outcome.Done

        driver.state.stack.count { driver.state.getEntity(it)?.has<CopyOfComponent>() == true } shouldBe 1
        driver.resolveAll(opponent)
        driver.getLifeTotal(caster) shouldBe 19
        driver.getLifeTotal(opponent) shouldBe 19
    }

    test("casting without replicate puts no copy trigger on the stack") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.putLandOnBattlefield(caster, "Mountain")
        val ping = driver.putCardInHand(caster, "Test Replicate Ping")

        driver.cast(ping, opponent, times = 1, slot = null).outcome shouldBe Outcome.Done
        driver.state.stack.size shouldBe 1
        driver.resolveAll(opponent) shouldBe 0
        driver.getLifeTotal(opponent) shouldBe 19
    }

    test("a replicate count the caster can't pay for is rejected") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        repeat(2) { driver.putLandOnBattlefield(caster, "Mountain") }
        val ping = driver.putCardInHand(caster, "Test Replicate Ping")

        driver.cast(ping, opponent, times = 2).outcome shouldNotBe Outcome.Done
    }

    test("a once-only optional cost can't be declared more than once, and a count must be positive") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        repeat(4) { driver.putLandOnBattlefield(caster, "Mountain") }
        val kicked = driver.putCardInHand(caster, "Test Kicker Ping")
        val ping = driver.putCardInHand(caster, "Test Replicate Ping")

        driver.cast(kicked, opponent, times = 2, slot = ChoiceSlot.KICKED).outcome shouldNotBe Outcome.Done
        driver.cast(ping, opponent, times = 0).outcome shouldNotBe Outcome.Done
    }

    test("an energy replicate cost pays three energy per copy") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val opponent = driver.getOpponent(caster)
        driver.putLandOnBattlefield(caster, "Mountain")
        driver.giveEnergy(caster, 7)
        val ping = driver.putCardInHand(caster, "Test Energy Replicate Ping")

        driver.cast(ping, opponent, times = 3).outcome shouldNotBe Outcome.Done
        driver.energy(caster) shouldBe 7

        driver.cast(ping, opponent, times = 2).outcome shouldBe Outcome.Done
        driver.energy(caster) shouldBe 1
        driver.resolveAll(opponent) shouldBe 2
        driver.getLifeTotal(opponent) shouldBe 17
    }

    test("the enumerator offers one cast per affordable replicate count") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        repeat(3) { driver.putLandOnBattlefield(caster, "Mountain") }
        driver.putCardInHand(caster, "Test Replicate Ping")

        val actions = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, caster)
        val replicated = actions.mapNotNull { a ->
            (a.action as? CastSpell)?.takeIf { it.declaredCostSlot == ChoiceSlot.REPLICATED }?.let { it.declaredCostTimes to a }
        }
        replicated.map { it.first } shouldBe listOf(1, 2)
        replicated.all { it.second.affordable } shouldBe true
        replicated.map { it.second.description } shouldBe listOf(
            "Cast Test Replicate Ping (Replicate ×1)",
            "Cast Test Replicate Ping (Replicate ×2)",
        )
    }

    test("the enumerator caps an energy replicate at the energy the caster has") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        driver.putLandOnBattlefield(caster, "Mountain")
        driver.giveEnergy(caster, 6)
        driver.putCardInHand(caster, "Test Energy Replicate Ping")

        val actions = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, caster)
        actions.mapNotNull { (it.action as? CastSpell)?.takeIf { c -> c.declaredCostSlot == ChoiceSlot.REPLICATED }?.declaredCostTimes }
            .shouldBe(listOf(1, 2))
    }
})
