package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dom.cards.SageOfLatNam
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Covers activating an ability with a "Sacrifice an artifact/creature" additional cost
 * (Sage of Lat-Nam: "{T}, Sacrifice an artifact: Draw a card").
 *
 * Three behaviours are pinned:
 *   1. Forced case (exactly one legal sacrifice candidate) — the engine auto-sacrifices it and
 *      draws a card with no decision and no failure.
 *   2. Choice case (two+ candidates) — the engine pauses with a SelectCards decision; after the
 *      player chooses one, that one is sacrificed and a card drawn.
 *   3. AI / loop regression — a bare ActivateAbility (empty costPayment) with exactly one candidate
 *      resolves successfully (previously the cost path failed "Not enough sacrifice targets chosen"
 *      forever, spinning the server's AI fallback loop).
 */
class SacrificeCostActivationScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SageOfLatNam))
        return driver
    }

    val abilityId = SageOfLatNam.activatedAbilities.first().id

    fun GameTestDriver.readySage(playerId: com.wingedsheep.sdk.model.EntityId): com.wingedsheep.sdk.model.EntityId {
        val sage = putCreatureOnBattlefield(playerId, "Sage of Lat-Nam")
        replaceState(state.updateEntity(sage) { it.without<SummoningSicknessComponent>() })
        return sage
    }

    test("Forced case: exactly one artifact auto-sacrifices and draws, no decision needed") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Forest" to 30))
        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sage = driver.readySage(alice)
        val artifact = driver.putPermanentOnBattlefield(alice, "Artifact Creature")

        val handBefore = driver.getHandSize(alice)

        val result = driver.submit(
            ActivateAbility(
                playerId = alice,
                sourceId = sage,
                abilityId = abilityId,
                costPayment = null,
            )
        )

        result.error shouldBe null
        driver.pendingDecision.shouldBeNull()
        // The single artifact was sacrificed (cost paid on activation, no longer on battlefield).
        driver.state.getBattlefield(alice) shouldNotContain artifact
        // Resolve Sage's ability off the stack so "Draw a card" happens.
        driver.bothPass()
        driver.getHandSize(alice) shouldBe handBefore + 1
    }

    test("Choice case: two artifacts pause for a SelectCards decision; chosen one is sacrificed") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Forest" to 30))
        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sage = driver.readySage(alice)
        val artifactA = driver.putPermanentOnBattlefield(alice, "Artifact Creature")
        val artifactB = driver.putPermanentOnBattlefield(alice, "Artifact Creature")

        val handBefore = driver.getHandSize(alice)

        val result = driver.submit(
            ActivateAbility(
                playerId = alice,
                sourceId = sage,
                abilityId = abilityId,
                costPayment = null,
            )
        )
        result.error shouldBe null

        val pending = driver.pendingDecision
        pending.shouldNotBeNull()
        val selection = pending.shouldBeInstanceOf<SelectCardsDecision>()
        selection.playerId shouldBe alice
        selection.minSelections shouldBe 1
        selection.maxSelections shouldBe 1
        selection.options shouldContainAll listOf(artifactA, artifactB)

        // Choose artifactA to sacrifice.
        val resumed = driver.submitCardSelection(alice, listOf(artifactA))
        resumed.error shouldBe null

        driver.state.getBattlefield(alice) shouldNotContain artifactA
        driver.state.getBattlefield(alice) shouldContain artifactB
        // Resolve Sage's ability off the stack so "Draw a card" happens.
        driver.bothPass()
        driver.getHandSize(alice) shouldBe handBefore + 1
    }

    test("AI / loop regression: bare activation with one candidate succeeds (no infinite failure)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Forest" to 30))
        val alice = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sage = driver.readySage(alice)
        val artifact = driver.putPermanentOnBattlefield(alice, "Artifact Creature")

        val handBefore = driver.getHandSize(alice)

        // Simulate the AI submitting a bare action with no sacrifice chosen.
        val result = driver.submit(
            ActivateAbility(
                playerId = alice,
                sourceId = sage,
                abilityId = abilityId,
                costPayment = com.wingedsheep.sdk.scripting.AdditionalCostPayment(),
            )
        )

        result.error shouldBe null
        driver.pendingDecision.shouldBeNull()
        driver.state.getBattlefield(alice) shouldNotContain artifact
        // Resolve Sage's ability off the stack so "Draw a card" happens.
        driver.bothPass()
        driver.getHandSize(alice) shouldBe handBefore + 1
    }
})
