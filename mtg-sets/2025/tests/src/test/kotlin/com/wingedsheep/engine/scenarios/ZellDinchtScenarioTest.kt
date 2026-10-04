package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.ZellDincht
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome
import io.kotest.matchers.shouldNotBe

/**
 * Zell Dincht (FIN) — {2}{R} Legendary Creature — Human Monk 0/3.
 *
 *  "You may play an additional land on each of your turns.
 *   Zell Dincht gets +1/+0 for each land you control.
 *   At the beginning of your end step, return a land you control to its owner's hand."
 *
 * Covers the +1/+0-per-land continuous self-buff, the extra land drop, and the forced end-step
 * land bounce.
 */
class ZellDinchtScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ZellDincht)
        driver.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("gets +1/+0 for each land you control") {
        val driver = newDriver()
        val me = driver.player1

        val zell = driver.putCreatureOnBattlefield(me, "Zell Dincht")

        withClue("No lands → base 0/3") {
            driver.state.projectedState.getPower(zell) shouldBe 0
            driver.state.projectedState.getToughness(zell) shouldBe 3
        }

        repeat(3) { driver.putLandOnBattlefield(me, "Mountain") }
        // An opponent's land must NOT count toward Zell's power.
        driver.putLandOnBattlefield(driver.player2, "Mountain")

        withClue("Three lands you control → +3/+0 = 3/3") {
            driver.state.projectedState.getPower(zell) shouldBe 3
            driver.state.projectedState.getToughness(zell) shouldBe 3
        }
    }

    test("lets you play an additional land each turn") {
        val driver = newDriver()
        val me = driver.player1
        driver.putCreatureOnBattlefield(me, "Zell Dincht")

        val land1 = driver.putCardInHand(me, "Mountain")
        driver.playLand(me, land1).outcome shouldBe Outcome.Done
        driver.state.getEntity(me)?.get<LandDropsComponent>()?.remaining shouldBe 0

        // The static bonus grants one more land play this turn.
        val land2 = driver.putCardInHand(me, "Mountain")
        driver.playLand(me, land2).outcome shouldBe Outcome.Done

        // Now the extra drop is consumed; a third land play is illegal.
        val land3 = driver.putCardInHand(me, "Mountain")
        driver.submitExpectFailure(com.wingedsheep.engine.core.PlayLand(me, land3)).outcome shouldNotBe Outcome.Done
    }

    test("returns a land you control to its owner's hand at your end step") {
        val driver = newDriver()
        val me = driver.player1

        driver.putCreatureOnBattlefield(me, "Zell Dincht")
        val land = driver.putLandOnBattlefield(me, "Mountain")
        val kept = driver.putLandOnBattlefield(me, "Mountain")
        val handBefore = driver.getHandSize(me)

        // Advance to the end step. The bounce doesn't target (CR 115.10a), so the land is chosen
        // as the trigger resolves, not when it goes on the stack.
        driver.passPriorityUntil(Step.END)
        repeat(20) {
            if (!driver.state.getZone(me, Zone.BATTLEFIELD).contains(land)) return@repeat
            when (val pending = driver.pendingDecision) {
                is ChooseTargetsDecision -> error("The bounce must not target")
                is SelectCardsDecision -> {
                    pending.options.toSet() shouldBe setOf(land, kept)
                    driver.submitCardSelection(pending.playerId, listOf(land))
                }
                null -> {
                    if (driver.state.stack.isEmpty()) return@repeat
                    driver.bothPass()
                }
                else -> driver.autoResolveDecision()
            }
        }

        withClue("The chosen land left the battlefield for its owner's hand") {
            driver.state.getZone(me, Zone.BATTLEFIELD).contains(land) shouldBe false
            driver.state.getZone(me, Zone.HAND).contains(land) shouldBe true
            driver.getHandSize(me) shouldBe handBefore + 1
            driver.state.getZone(me, Zone.BATTLEFIELD).contains(kept) shouldBe true
        }
    }
})
