package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.CaptainStormCosmiumRaider
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Captain Storm, Cosmium Raider (LCI #227).
 *
 * "Whenever an artifact you control enters, put a +1/+1 counter on target Pirate you control."
 *
 * Proves three scenarios:
 *  1. Happy path — artifact enters → trigger fires → target Storm (itself a Pirate) →
 *     Storm gains a +1/+1 counter.
 *  2. Two artifacts entering in the same turn → two separate triggers, two counters.
 *  3. Different target Pirate — when a second Pirate (Ragavan) is on the battlefield the
 *     trigger can target it instead, leaving Storm with no counter.
 */
class CaptainStormCosmiumRaiderScenarioTest : FunSpec({

    // A simple {0} artifact used to fire the "artifact you control enters" trigger.
    val testArtifact = card("Test Artifact") {
        manaCost = "{0}"
        typeLine = "Artifact"
        oracleText = ""
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(CaptainStormCosmiumRaider)
        driver.registerCard(testArtifact)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun plusOneCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    // ──────────────────────────────────────────────────────────────────────────
    // Test 1: happy path — artifact enters, trigger fires, Captain Storm is targeted
    // ──────────────────────────────────────────────────────────────────────────

    test("an artifact you control entering fires the trigger; Storm (a Pirate) gains a +1/+1 counter") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        // Captain Storm is itself a Legendary Creature — Human Pirate; it is a legal target.
        val captain = driver.putCreatureOnBattlefield(me, "Captain Storm, Cosmium Raider")

        // Cast the free artifact to fire the ETB trigger.
        val artifact = driver.putCardInHand(me, "Test Artifact")
        driver.castSpell(me, artifact).isSuccess shouldBe true
        driver.bothPass() // artifact resolves, ETB trigger put on stack

        // Engine pauses for target selection.
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, listOf(captain))
        driver.bothPass() // trigger resolves

        plusOneCounters(driver, captain) shouldBe 1
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 2: two artifacts entering in the same turn → two separate triggers
    // ──────────────────────────────────────────────────────────────────────────

    test("each artifact entering fires a separate trigger; two artifacts yield two counters on Storm") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val captain = driver.putCreatureOnBattlefield(me, "Captain Storm, Cosmium Raider")

        // First artifact.
        driver.castSpell(me, driver.putCardInHand(me, "Test Artifact")).isSuccess shouldBe true
        driver.bothPass() // first artifact resolves
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, listOf(captain))
        driver.bothPass() // first trigger resolves → 1 counter

        plusOneCounters(driver, captain) shouldBe 1

        // Second artifact.
        driver.castSpell(me, driver.putCardInHand(me, "Test Artifact")).isSuccess shouldBe true
        driver.bothPass() // second artifact resolves
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, listOf(captain))
        driver.bothPass() // second trigger resolves → 2 counters

        plusOneCounters(driver, captain) shouldBe 2
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 3: any Pirate you control is a valid target, not just Storm itself
    // ──────────────────────────────────────────────────────────────────────────

    test("trigger can target any Pirate you control, not only Captain Storm itself") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val captain = driver.putCreatureOnBattlefield(me, "Captain Storm, Cosmium Raider")
        // Ragavan is a Monkey Pirate; it is a valid target for the trigger.
        val ragavan = driver.putCreatureOnBattlefield(me, "Test Hasty Prospector")

        val artifact = driver.putCardInHand(me, "Test Artifact")
        driver.castSpell(me, artifact).isSuccess shouldBe true
        driver.bothPass() // artifact resolves, trigger fires

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        // Target Ragavan instead of Storm.
        driver.submitTargetSelection(me, listOf(ragavan))
        driver.bothPass() // trigger resolves

        // Ragavan received the counter; Storm did not.
        plusOneCounters(driver, ragavan) shouldBe 1
        plusOneCounters(driver, captain) shouldBe 0
    }
})
