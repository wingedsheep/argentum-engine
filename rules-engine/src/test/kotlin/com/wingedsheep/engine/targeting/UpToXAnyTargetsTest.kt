package com.wingedsheep.engine.targeting

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.event.GlobalGrantedTriggeredAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Each of up to X targets" — an [com.wingedsheep.sdk.scripting.targets.AnyTarget] whose maximum is
 * a [DynamicAmount] (`Targets.AnyUpTo`). The cast-time X path is pinned by Crackle with Power's own
 * scenario test; this file pins the rest of the shape:
 *
 *  - CR 603.3d (applying 601.2c to a trigger going on the stack): a board-read count is fixed when
 *    the trigger is put on the stack, so the decision offers exactly that many slots, players
 *    included, and zero is a legal choice ("up to").
 *  - An "any other target" wrapper keeps the dynamic cap.
 */
class UpToXAnyTargetsTest : FunSpec({

    fun driverWithEndStepPing(lands: Int): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        repeat(lands) { driver.putLandOnBattlefield(you, "Forest") }
        // "At your end step, this deals 1 damage to each of up to X targets, where X is the number
        // of lands you control."
        val ability = TriggeredAbility.create(
            id = AbilityId("UpToXAnyTargetsTest_ping"),
            trigger = EventPattern.StepEvent(Step.END, Player.You),
            effect = Effects.ForEachTarget(Effects.DealDamage(1, EffectTarget.ContextTarget(0))),
            targetRequirement = Targets.AnyUpTo(DynamicAmounts.landsYouControl()),
        )
        driver.replaceState(
            driver.state.copy(
                globalGrantedTriggeredAbilities = listOf(
                    GlobalGrantedTriggeredAbility(
                        ability = ability,
                        controllerId = you,
                        sourceId = you,
                        sourceName = "Test Pinger",
                        duration = Duration.Permanent
                    )
                )
            )
        )
        return driver
    }

    test("the trigger's cap is the board count when it goes on the stack, and players are legal targets") {
        val driver = driverWithEndStepPing(lands = 2)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.passPriorityUntil(Step.END)
        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val req = decision.targetRequirements.single()
        withClue("two lands → up to two targets") {
            req.maxTargets shouldBe 2
            req.minTargets shouldBe 0
        }
        decision.legalTargets.getValue(0) shouldContain opponent
        decision.legalTargets.getValue(0) shouldContain bear

        driver.submitTargetSelection(you, listOf(opponent, bear)).error shouldBe null
        driver.bothPass()
        driver.getLifeTotal(opponent) shouldBe 19
        (driver.state.getEntity(bear)?.get<DamageComponent>()?.amount ?: 0) shouldBe 1
    }

    test("a third target past the cap is rejected") {
        val driver = driverWithEndStepPing(lands = 2)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.passPriorityUntil(Step.END)
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(you, listOf(opponent, bear, you)).error shouldNotBe null
    }

    test("'up to': choosing no targets is legal and the ability does nothing") {
        val driver = driverWithEndStepPing(lands = 1)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        driver.passPriorityUntil(Step.END)
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(you, emptyList()).error shouldBe null
        driver.bothPass()
        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("an 'any other target' wrapper keeps the dynamic cap and its wording") {
        val upToX = Targets.AnyUpTo(DynamicAmount.XValue)
        val other = TargetOther(upToX)
        other.dynamicMaxCount shouldBe DynamicAmount.XValue
        upToX.description shouldBe "up to X targets"
        other.effectiveMinCount shouldBe 0
    }
})
