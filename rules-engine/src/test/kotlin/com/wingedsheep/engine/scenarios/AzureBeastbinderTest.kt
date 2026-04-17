package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.AzureBeastbinder
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.CarrotCake
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.LifecreedDuo
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Azure Beastbinder (BLB).
 *
 * Whenever this creature attacks, up to one target artifact, creature, or planeswalker
 * an opponent controls loses all abilities until your next turn. If it's a creature,
 * it also has base power and toughness 2/2 until your next turn.
 *
 * Regression guard: when the target is a non-creature artifact, only the
 * ability-removal half of the effect should apply. The "if it's a creature" branch of
 * SetBasePowerToughnessEffect must not turn the artifact into a 2/2 or otherwise
 * change its type.
 */
class AzureBeastbinderTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AzureBeastbinder, CarrotCake, LifecreedDuo))
        return driver
    }

    // Player 1 may not be the starting player (random first turn); advance until it is.
    fun GameTestDriver.advanceToPlayer1DeclareAttackers() {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.DECLARE_ATTACKERS)
            safety++
        }
    }

    test("non-creature artifact target loses abilities but does NOT become a 2/2") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val p1 = driver.player1
        val p2 = driver.player2

        val beastbinder = driver.putCreatureOnBattlefield(p1, "Azure Beastbinder")
        driver.removeSummoningSickness(beastbinder)
        val carrotCake = driver.putPermanentOnBattlefield(p2, "Carrot Cake")

        driver.advanceToPlayer1DeclareAttackers()

        // Sanity: before the trigger resolves, Carrot Cake is a non-creature artifact
        // with intact abilities and no P/T.
        val before = projector.project(driver.state)
        before.hasType(carrotCake, "ARTIFACT") shouldBe true
        before.isCreature(carrotCake) shouldBe false
        before.hasLostAllAbilities(carrotCake) shouldBe false
        before.getPower(carrotCake).shouldBeNull()
        before.getToughness(carrotCake).shouldBeNull()

        // Beastbinder attacks alone; the "whenever this creature attacks" trigger fires
        // and pauses for target selection.
        driver.declareAttackers(p1, listOf(beastbinder), p2)

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(p1, listOf(carrotCake))

        // Resolve the trigger.
        driver.bothPass()

        val after = projector.project(driver.state)

        // Abilities are stripped.
        after.hasLostAllAbilities(carrotCake) shouldBe true

        // The artifact is still an artifact — the effect does not add the creature type.
        after.hasType(carrotCake, "ARTIFACT") shouldBe true
        after.isCreature(carrotCake) shouldBe false

        // The "if it's a creature, base P/T becomes 2/2" branch is a resolution-time
        // conditional — on a non-creature artifact it must be a complete no-op. No
        // SetPowerToughness floating effect should be created and the projector should
        // not assign P/T to the artifact.
        after.getPower(carrotCake).shouldBeNull()
        after.getToughness(carrotCake).shouldBeNull()
    }

    test("baseline: Lifecreed Duo's ETB trigger fires when its abilities are intact") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )

        val p1 = driver.player1
        val p2 = driver.player2

        driver.putCreatureOnBattlefield(p2, "Lifecreed Duo")
        val savannah = driver.putCardInHand(p2, "Savannah Lions")

        // Advance to p2's first main phase (skip past whatever turn we start on).
        while (driver.activePlayer != p2) {
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            if (driver.activePlayer != p2) driver.bothPass()
        }

        val lifeBefore = driver.getLifeTotal(p2)
        driver.giveMana(p2, Color.WHITE)
        driver.castSpell(p2, savannah)
        driver.bothPass()  // resolve Savannah Lions
        driver.bothPass()  // resolve the Lifecreed Duo trigger that fires on its ETB

        // Lifecreed Duo's "whenever another creature you control enters" trigger
        // resolves: gain 1 life.
        driver.getLifeTotal(p2) shouldBe lifeBefore + 1
    }

    test("triggered ability of a stripped creature does not fire") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )

        val p1 = driver.player1
        val p2 = driver.player2

        val beastbinder = driver.putCreatureOnBattlefield(p1, "Azure Beastbinder")
        driver.removeSummoningSickness(beastbinder)
        val lifecreed = driver.putCreatureOnBattlefield(p2, "Lifecreed Duo")
        val savannah = driver.putCardInHand(p2, "Savannah Lions")

        driver.advanceToPlayer1DeclareAttackers()

        // Beastbinder attacks alone; the on-attack trigger strips Lifecreed Duo's
        // abilities (Flying + the gain-1-life trigger) until p1's next turn.
        driver.declareAttackers(p1, listOf(beastbinder), p2)
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(p1, listOf(lifecreed))
        driver.bothPass()

        projector.project(driver.state).hasLostAllAbilities(lifecreed) shouldBe true

        // Advance into p2's precombat main. Capture life AFTER any combat damage
        // settles so the assertion isolates the trigger check from combat.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.activePlayer shouldBe p2

        val lifeBefore = driver.getLifeTotal(p2)

        // p2 casts Savannah Lions — "another creature you control entered". With its
        // abilities stripped, Lifecreed Duo's trigger must be suppressed.
        driver.giveMana(p2, Color.WHITE)
        driver.castSpell(p2, savannah)
        driver.bothPass()

        driver.getLifeTotal(p2) shouldBe lifeBefore
    }
})
