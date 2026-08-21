package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.QuicksilverFountain
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Quicksilver Fountain (MRD #233) — "At the beginning of each player's upkeep, that player puts a
 * flood counter on target non-Island land they control of their choice. That land is an Island for
 * as long as it has a flood counter on it. At the beginning of each end step, if all lands on the
 * battlefield are Islands, remove all flood counters from them."
 *
 * The load-bearing claim is the routing: the trigger belongs to the Fountain's controller but the
 * *choice* belongs to whoever's upkeep it is, over a land **they** control. That is
 * `TargetChooser.TriggeringPlayer`, and before it existed the engine handed every trigger's target
 * decision to the ability's controller — so a Fountain you played would have let *you* pick which of
 * your opponent's lands drowned. These tests pin the deciding player explicitly, not just the
 * outcome, because in a mirror board the outcome alone can't tell the two apart.
 */
class QuicksilverFountainScenarioTest : FunSpec({

    // Built once, during spec construction: `TestCards.all` forces a ClassGraph scan of the
    // whole card corpus, and paying that inside the first test body puts it under the per-test
    // timeout — which is what makes a single-spec run flake on a loaded machine.
    val cards = TestCards.all + QuicksilverFountain

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(cards)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /**
     * Pass priority until a decision is raised, without `passPriorityUntil`'s auto-resolve — which
     * would answer the Fountain's own target decision before the test could look at it.
     */
    fun GameTestDriver.passUntilDecision(maxPasses: Int = 60) {
        repeat(maxPasses) {
            if (state.pendingDecision != null) return
            state.priorityPlayerId?.let { passPriority(it) }
        }
        error("no decision was raised within $maxPasses passes (step ${state.step})")
    }

    fun GameTestDriver.floodCounters(land: EntityId): Int =
        state.getEntity(land)?.get<CountersComponent>()?.getCount(CounterType.FLOOD) ?: 0

    fun GameTestDriver.isIsland(land: EntityId): Boolean =
        state.projectedState.getSubtypes(land).any { it.equals("Island", ignoreCase = true) }

    test("the upkeep choice is made by the player whose upkeep it is, over their own land") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Quicksilver Fountain")
        val mine = d.putLandOnBattlefield(d.player1, "Forest")
        val theirs = d.putLandOnBattlefield(d.player2, "Forest")

        // Roll into player 2's upkeep — player 1's has already passed this turn.
        d.passUntilDecision()

        val decision = d.state.pendingDecision
        decision shouldNotBe null
        withClue("\"that player … of their choice\" — the decision goes to the active player, not to the Fountain's controller") {
            decision!!.playerId shouldBe d.player2
        }
        withClue("and only over a land THEY control") {
            val targets = (decision as ChooseTargetsDecision).legalTargets.values.flatten()
            targets shouldBe listOf(theirs)
        }

        d.submitTargetSelection(d.player2, listOf(theirs)).error shouldBe null
        // Let the trigger resolve.
        repeat(4) { if (d.state.pendingDecision == null) d.bothPass() }

        withClue("the flood counter landed on the choosing player's land") {
            d.floodCounters(theirs) shouldBe 1
            d.floodCounters(mine) shouldBe 0
        }
        withClue("\"that land is an Island for as long as it has a flood counter on it\"") {
            d.isIsland(theirs) shouldBe true
            d.isIsland(mine) shouldBe false
        }
    }

    test("an already-flooded land is not offered again — it is an Island now") {
        // The Layer 4 static is what enforces this, not a card-level exclusion: "non-Island land"
        // reads projected subtypes, and a flooded land projects as an Island. It is why the card
        // spreads across a board instead of piling counters on one land.
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Quicksilver Fountain")
        val flooded = d.putLandOnBattlefield(d.player2, "Forest")
        d.addComponent(flooded, CountersComponent(mapOf(CounterType.FLOOD to 1)))
        val dry = d.putLandOnBattlefield(d.player2, "Forest")

        d.passUntilDecision()

        val decision = d.state.pendingDecision as ChooseTargetsDecision
        decision.playerId shouldBe d.player2
        withClue("the flooded Forest is an Island, so only the dry one is a legal target") {
            decision.legalTargets.values.flatten() shouldBe listOf(dry)
        }
    }

    test("the end-step sweep removes every flood counter once all lands are Islands") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Quicksilver Fountain")
        val mine = d.putLandOnBattlefield(d.player1, "Forest")
        val theirs = d.putLandOnBattlefield(d.player2, "Forest")
        d.addComponent(mine, CountersComponent(mapOf(CounterType.FLOOD to 1)))
        d.addComponent(theirs, CountersComponent(mapOf(CounterType.FLOOD to 1)))

        withClue("both lands are Islands only because of their counters") {
            d.isIsland(mine) shouldBe true
            d.isIsland(theirs) shouldBe true
        }

        d.passPriorityUntil(Step.END)
        d.bothPass()

        withClue("\"remove all flood counters from them\" is plural and crosses both battlefields") {
            d.floodCounters(mine) shouldBe 0
            d.floodCounters(theirs) shouldBe 0
        }
        withClue("and the lands stop being Islands the moment the counters go") {
            d.isIsland(mine) shouldBe false
            d.isIsland(theirs) shouldBe false
        }
    }

    test("the sweep does not fire while any non-Island land is on the battlefield") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Quicksilver Fountain")
        val flooded = d.putLandOnBattlefield(d.player1, "Forest")
        d.addComponent(flooded, CountersComponent(mapOf(CounterType.FLOOD to 1)))
        // One dry Forest anywhere is enough to hold the intervening-if false (CR 603.4).
        d.putLandOnBattlefield(d.player2, "Forest")

        d.passPriorityUntil(Step.END)
        d.bothPass()

        withClue("the counter survives — the condition counts every player's lands, not just yours") {
            d.floodCounters(flooded) shouldBe 1
            d.isIsland(flooded) shouldBe true
        }
    }
})
