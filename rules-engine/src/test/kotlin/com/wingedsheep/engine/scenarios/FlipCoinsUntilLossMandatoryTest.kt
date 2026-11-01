package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.core.GameLimits
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.composite.FlipCoinsUntilLossExecutor
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import com.wingedsheep.sdk.scripting.FlipAdditionalCoins
import com.wingedsheep.sdk.scripting.WinCoinFlips
import com.wingedsheep.sdk.scripting.effects.FlipCoinsUntilLossEffect
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * [FlipCoinsUntilLossEffect] with `mayStop = false` — "flip a coin until you lose a flip" with no
 * option to stop (Mirror March), against the optional form "…or choose to stop flipping" (Fiery
 * Gambit).
 *
 * The rules pinned here:
 * - the mandatory run never asks "flip again?": a won flip is followed by the next flip;
 * - it ends on the first lost flip, and the lost flip is not counted in the tally;
 * - when no flip can be lost (a forced-win static), the run stops at the engine backstop instead of
 *   looping forever;
 * - under a flip-additional-coins replacement (Krark's Thumb) the "which coin do you keep?" pause
 *   resumes into the same mandatory run — the only questions ever raised are keep-heads/keep-tails.
 */
class FlipCoinsUntilLossMandatoryTest : FunSpec({

    val luckyCoin = card("Test Lucky Coin") {
        typeLine = "Artifact"
        manaCost = "{2}"
        oracleText = "You win all coin flips."
        staticAbility { ability = WinCoinFlips() }
    }

    val thumb = card("Test Thumb") {
        typeLine = "Legendary Artifact"
        manaCost = "{2}"
        oracleText = "If you would flip a coin, instead flip two coins and ignore one."
        staticAbility { ability = FlipAdditionalCoins(coinsPerFlip = 2) }
    }

    /** Gain 1 life per flip won, so the published tally is readable off the life total. */
    val marchGambit = card("Test March Gambit") {
        typeLine = "Sorcery"
        manaCost = "{R}"
        oracleText = "Flip a coin until you lose a flip. You gain 1 life for each flip you won."
        spell {
            effect = Effects.FlipCoinsUntilLoss(storeWinsAs = "w", mayStop = false) then
                Effects.GainLife(DynamicAmounts.storedNumber("w"))
        }
    }

    fun driverWith(vararg battlefield: String): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        listOf(luckyCoin, thumb, marchGambit).forEach { driver.registerCard(it) }
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(com.wingedsheep.sdk.core.Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!
        battlefield.forEach { driver.putPermanentOnBattlefield(player, it) }
        return driver to player
    }

    fun run(driver: GameTestDriver, player: EntityId, effect: FlipCoinsUntilLossEffect, seed: Long) =
        FlipCoinsUntilLossExecutor(driver.cardRegistry).execute(
            driver.state.copy(rng = GameRng.seeded(seed)),
            effect,
            EffectContext(sourceId = null, controllerId = player)
        )

    test("the mandatory run never pauses and ends on the first lost flip, which is not counted") {
        val (driver, player) = driverWith()
        val mandatory = FlipCoinsUntilLossEffect(storeWinsAs = "w", mayStop = false)

        var longestRun = 0
        (1L..60L).forEach { seed ->
            val result = run(driver, player, mandatory, seed)
            withClue("seed $seed") {
                (result.outcome is Outcome.Paused) shouldBe false
                val flips = result.events.filterIsInstance<CoinFlipEvent>()
                flips.last().won shouldBe false
                flips.dropLast(1).all { it.won } shouldBe true
                result.updatedStoredNumbers["w"] shouldBe flips.size - 1
                longestRun = maxOf(longestRun, flips.size - 1)
            }
        }
        // Some seed in the range won at least twice in a row, so "keeps flipping after a win" was
        // exercised for real rather than only the lose-first path.
        longestRun shouldBeGreaterThanOrEqual 2
    }

    test("the optional form asks after a won flip; the mandatory form does not on the same seed") {
        val (driver, player) = driverWith()
        val optional = FlipCoinsUntilLossEffect(storeWinsAs = "w")
        val mandatory = optional.copy(mayStop = false)

        val seed = (1L..60L).first {
            run(driver, player, mandatory, it).updatedStoredNumbers["w"]!! >= 1
        }
        val asked = run(driver, player, optional, seed)
        (asked.pendingDecision as? YesNoDecision)?.yesText shouldBe "Flip again"
        (run(driver, player, mandatory, seed).outcome is Outcome.Paused) shouldBe false

        mandatory.description shouldBe "Flip a coin until you lose a flip"
        optional.description shouldBe "Flip a coin until you lose a flip or choose to stop flipping"
    }

    test("when no flip can be lost the mandatory run stops at the backstop instead of looping") {
        val (driver, player) = driverWith("Test Lucky Coin")
        val result = run(driver, player, FlipCoinsUntilLossEffect("w", mayStop = false), 7L)

        (result.outcome is Outcome.Paused) shouldBe false
        result.updatedStoredNumbers["w"] shouldBe GameLimits.MAX_COIN_FLIPS_PER_EFFECT
        result.events.filterIsInstance<CoinFlipEvent>().all { it.won } shouldBe true
    }

    test("a Thumb's keep-which-coin pause resumes into the same mandatory run") {
        var sawChoice = false
        (1L..30L).forEach { seed ->
            val (driver, player) = driverWith("Test Thumb")
            val spell = driver.putCardInHand(player, "Test March Gambit")
            driver.giveMana(player, Color.RED, 1)
            driver.replaceState(driver.state.copy(rng = GameRng.seeded(seed)))
            val lifeBefore = driver.getLifeTotal(player)

            driver.castSpell(player, spell).outcome shouldBe Outcome.Done
            driver.bothPass()

            // Keep heads twice, then tails — a mixed batch is the only thing ever asked.
            var keptHeads = 0
            var answered = 0
            while (driver.pendingDecision != null) {
                val decision = driver.pendingDecision as? YesNoDecision
                decision.shouldNotBeNull()
                withClue("seed $seed") { decision.yesText shouldBe "Keep heads" }
                sawChoice = true
                val keepHeads = keptHeads < 2
                if (keepHeads) keptHeads++
                driver.submitYesNo(player, keepHeads)
                answered++
                withClue("seed $seed ran away") { (answered < 200) shouldBe true }
            }
            // Every heads answer was a won flip that the run counted; unanimous-heads batches add more.
            driver.getLifeTotal(player) shouldBeGreaterThanOrEqual lifeBefore + keptHeads
        }
        sawChoice shouldBe true
    }
})
