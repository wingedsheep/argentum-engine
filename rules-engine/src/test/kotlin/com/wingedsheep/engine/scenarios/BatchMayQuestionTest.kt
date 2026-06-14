package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.BatchYesNoDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.MayEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for feature B — batched may-question (backlog/stack-collapse-and-batch-decisions.md §B).
 *
 * When a run of structurally identical optional ("you may … target …") triggers fires off one
 * event, the controller answers a single [BatchYesNoDecision] instead of one yes/no per trigger.
 * The guard (MTGO "auto-stack identical triggers"): only same-controller, same-[AbilityIdentity]
 * triggers are batched, and only the *yes/no* is shared — each instance still picks its own target.
 *
 * The board: N copies of "Batch Pinger" ("Whenever another creature you control enters, you may
 * have Batch Pinger deal 1 damage to any target") plus a vanilla creature whose entry fires them
 * all at once.
 */
class BatchMayQuestionTest : FunSpec({

    val batchPinger = card("Batch Pinger") {
        manaCost = "{1}"
        typeLine = "Creature — Test"
        power = 1
        toughness = 1
        oracleText = "Whenever another creature you control enters the battlefield, you may have " +
            "Batch Pinger deal 1 damage to any target."
        triggeredAbility {
            trigger = Triggers.OtherCreatureEnters
            val t = target("target", Targets.Any)
            effect = MayEffect(Effects.DealDamage(1, t))
        }
    }

    // Vanilla creature whose entry fires every Batch Pinger's "another creature enters" trigger.
    val batchBear = card("Batch Bear") {
        manaCost = "{1}"
        typeLine = "Creature — Test"
        power = 2
        toughness = 2
    }

    fun driverWithPingers(count: Int): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(batchPinger, batchBear))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        repeat(count) { driver.putCreatureOnBattlefield(player, "Batch Pinger") }

        driver.giveColorlessMana(player, 1)
        val bear = driver.putCardInHand(player, "Batch Bear")
        driver.castSpell(player, bear).isSuccess shouldBe true
        driver.bothPass() // resolve the bear; it enters and every Batch Pinger triggers
        return Triple(driver, player, opponent)
    }

    test("two identical may+target triggers raise ONE BatchYesNoDecision, not two yes/no prompts") {
        val (driver, player, _) = driverWithPingers(2)

        val decision = driver.pendingDecision.shouldBeInstanceOf<BatchYesNoDecision>()
        decision.count shouldBe 2
        decision.playerId shouldBe player
        // Carries the shared ability identity (the C.2 key the grouping is built on).
        val identity = decision.context.abilityIdentity.shouldNotBeNull()
        identity.cardDefinitionId shouldBe "Batch Pinger"
    }

    test("yes to all — each instance still targets individually, both resolve") {
        val (driver, player, opponent) = driverWithPingers(2)

        driver.submitBatchYesNo(player, choice = true, applyToAll = true)

        // Each of the two triggers now asks for its own target.
        val t1 = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(t1.playerId, listOf(opponent))
        val t2 = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(t2.playerId, listOf(opponent))

        // Both pings are on the stack; resolve them.
        val onStack = driver.state.stack.mapNotNull {
            driver.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()
        }.filter { it.sourceName == "Batch Pinger" }
        onStack.size shouldBe 2

        driver.bothPass()
        driver.bothPass()

        driver.assertLifeTotal(opponent, 18) // 20 - 1 - 1
    }

    test("no to all — the whole run is declined, no targets asked, no damage") {
        val (driver, player, opponent) = driverWithPingers(2)

        driver.submitBatchYesNo(player, choice = false, applyToAll = true)

        // No further decision, nothing on the stack from the pingers, opponent untouched.
        driver.pendingDecision shouldBe null
        driver.state.stack.mapNotNull {
            driver.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()
        }.none { it.sourceName == "Batch Pinger" } shouldBe true
        driver.assertLifeTotal(opponent, 20)
    }

    test("peel-off — yes to this one targets it, the remaining run re-batches") {
        val (driver, player, opponent) = driverWithPingers(3)

        val batch = driver.pendingDecision.shouldBeInstanceOf<BatchYesNoDecision>()
        batch.count shouldBe 3

        // "Yes" (this one): peel one off and target it.
        driver.submitBatchYesNo(player, choice = true, applyToAll = false)
        val firstTarget = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(firstTarget.playerId, listOf(opponent))

        // The remaining two re-raise as a fresh batch of 2.
        val reBatch = driver.pendingDecision.shouldBeInstanceOf<BatchYesNoDecision>()
        reBatch.count shouldBe 2

        // Decline the rest; only the peeled ping resolves.
        driver.submitBatchYesNo(player, choice = false, applyToAll = true)
        driver.bothPass()
        driver.assertLifeTotal(opponent, 19) // exactly one ping landed
    }

    test("a single trigger is never batched — it raises an ordinary yes/no") {
        val (driver, _, _) = driverWithPingers(1)

        // One pinger → one may+target trigger → the plain per-trigger path, not a batch.
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
    }
})
