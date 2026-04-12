package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.DeepchannelDuelist
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.GlamerGifter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Glamer Gifter's ETB ability: sets base P/T to 4/4 and grants all creature types.
 *
 * Specifically tests that granting Changeling (all creature types) via a continuous effect
 * interacts correctly with lord effects that check subtypes in the layer system.
 */
class GlamerGifterTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GlamerGifter, DeepchannelDuelist))
        return driver
    }

    test("Glamer Gifter ETB sets target creature to 4/4 with all creature types") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Armored Pegasus (1/2 Flying) on the battlefield
        val pegasus = driver.putCreatureOnBattlefield(activePlayer, "Armored Pegasus")

        // Give player Glamer Gifter and mana to cast it
        val gifter = driver.putCardInHand(activePlayer, "Glamer Gifter")
        driver.giveMana(activePlayer, Color.BLUE, 2)

        // Cast Glamer Gifter
        val castResult = driver.castSpell(activePlayer, gifter)
        castResult.isSuccess shouldBe true

        // Resolve the creature spell
        driver.bothPass()

        // Glamer Gifter should be on the battlefield
        driver.findPermanent(activePlayer, "Glamer Gifter") shouldNotBe null

        // ETB trigger should pause for target selection
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        // Select Armored Pegasus as the target
        driver.submitTargetSelection(activePlayer, listOf(pegasus))

        // Resolve the triggered ability
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // Verify projected state: Armored Pegasus should be 4/4 with all creature types
        val projected = projector.project(driver.state)
        projected.getPower(pegasus) shouldBe 4
        projected.getToughness(pegasus) shouldBe 4
        projected.hasKeyword(pegasus, Keyword.CHANGELING) shouldBe true
        projected.hasSubtype(pegasus, "Merfolk") shouldBe true
        projected.hasSubtype(pegasus, "Goblin") shouldBe true
        projected.hasSubtype(pegasus, "Elf") shouldBe true
    }

    test("Glamer Gifter ETB + Merfolk lord makes target 5/5 via granted Changeling") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Deepchannel Duelist (Merfolk lord, other Merfolk +1/+1) on the battlefield
        val duelist = driver.putCreatureOnBattlefield(activePlayer, "Deepchannel Duelist")
        // Put Armored Pegasus (1/2) on the battlefield
        val pegasus = driver.putCreatureOnBattlefield(activePlayer, "Armored Pegasus")

        // Verify baseline: pegasus is 1/2 (not a Merfolk, no bonus from Duelist)
        val projectedBefore = projector.project(driver.state)
        projectedBefore.getPower(pegasus) shouldBe 1
        projectedBefore.getToughness(pegasus) shouldBe 2

        // Give player Glamer Gifter and mana to cast it
        val gifter = driver.putCardInHand(activePlayer, "Glamer Gifter")
        driver.giveMana(activePlayer, Color.BLUE, 2)

        // Cast Glamer Gifter
        driver.castSpell(activePlayer, gifter)

        // Resolve the creature spell
        driver.bothPass()

        // ETB trigger fires — select Armored Pegasus as target
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(activePlayer, listOf(pegasus))

        // Resolve the triggered ability
        if (driver.stackSize > 0) {
            driver.bothPass()
        }

        // Verify: Pegasus should be 5/5
        // Layer 6: Changeling keyword granted → all creature types (including Merfolk)
        // Layer 7b: Base P/T set to 4/4
        // Layer 7c: Deepchannel Duelist gives other Merfolk +1/+1 → 5/5
        val projected = projector.project(driver.state)
        projected.hasSubtype(pegasus, "Merfolk") shouldBe true
        projected.getPower(pegasus) shouldBe 5
        projected.getToughness(pegasus) shouldBe 5

        // Deepchannel Duelist itself should still be 2/2 (lord doesn't boost itself)
        projected.getPower(duelist) shouldBe 2
        projected.getToughness(duelist) shouldBe 2
    }
})
