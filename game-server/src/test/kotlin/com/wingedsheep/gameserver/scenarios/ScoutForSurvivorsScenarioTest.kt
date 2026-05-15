package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Scout for Survivors {2}{W} sorcery:
 *   "Return up to three target creature cards with total mana value 3 or less from your
 *    graveyard to the battlefield. Put a +1/+1 counter on each of them."
 *
 * Drives [com.wingedsheep.sdk.scripting.effects.SelectionRestriction.TotalManaValueAtMost]:
 * the executor's ceiling caps the selection at the greedy fit of cheapest cards, and the
 * continuation resumer trims any oversubmit in response order.
 */
class ScoutForSurvivorsScenarioTest : ScenarioTestBase() {

    init {
        test("three 1-MV creatures all return with a +1/+1 counter") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Scout for Survivors")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInGraveyard(1, "Hired Claw")    // {R} — MV 1
                .withCardInGraveyard(1, "Jungle Lion")   // {G} — MV 1
                .withCardInGraveyard(1, "Foothill Guide") // {W} — MV 1
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val claw = game.state.getGraveyard(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Hired Claw"
            }
            val lion = game.state.getGraveyard(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Jungle Lion"
            }
            val guide = game.state.getGraveyard(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Foothill Guide"
            }

            game.castSpell(1, "Scout for Survivors").error shouldBe null
            game.resolveStack()

            withClue("Should pause for card selection from graveyard") {
                game.hasPendingDecision() shouldBe true
            }

            game.selectCards(listOf(claw, lion, guide)).error shouldBe null

            withClue("All three 1-MV creatures (total 3) returned to battlefield") {
                game.isOnBattlefield("Hired Claw") shouldBe true
                game.isOnBattlefield("Jungle Lion") shouldBe true
                game.isOnBattlefield("Foothill Guide") shouldBe true
            }
            withClue("Each gained a +1/+1 counter") {
                val clawCounters = game.state.getEntity(game.findPermanent("Hired Claw")!!)?.get<CountersComponent>()
                val lionCounters = game.state.getEntity(game.findPermanent("Jungle Lion")!!)?.get<CountersComponent>()
                val guideCounters = game.state.getEntity(game.findPermanent("Foothill Guide")!!)?.get<CountersComponent>()
                clawCounters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                lionCounters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                guideCounters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }
        }

        test("running-sum trim: a later card that would overflow the cap is rejected, earlier picks resolve") {
            // Graveyard sized so the greedy ceiling allows 2 cards (1+2 fits, 1+2+3 doesn't).
            // The player submits [Horned Turtle (3), Hired Claw (1)] — within maxSelections (2) by
            // count but 3+1=4 over the cap. The continuation resumer must keep the Turtle and
            // drop the Claw, leaving the Claw in the graveyard.
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Scout for Survivors")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInGraveyard(1, "Horned Turtle")  // {2}{U} — MV 3
                .withCardInGraveyard(1, "Grizzly Bears")  // {1}{G} — MV 2
                .withCardInGraveyard(1, "Hired Claw")     // {R}    — MV 1
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val turtle = game.state.getGraveyard(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Horned Turtle"
            }
            val claw = game.state.getGraveyard(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Hired Claw"
            }

            game.castSpell(1, "Scout for Survivors").error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe true

            // Submit [Turtle (3), Claw (1)]. Running totals: 0+3=3 ✓, 3+1=4 ✗.
            // Expected kept: Turtle only.
            game.selectCards(listOf(turtle, claw)).error shouldBe null

            withClue("Horned Turtle (first pick, fits) is on battlefield with a +1/+1 counter") {
                game.isOnBattlefield("Horned Turtle") shouldBe true
                val counters = game.state.getEntity(game.findPermanent("Horned Turtle")!!)?.get<CountersComponent>()
                counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            }
            withClue("Hired Claw (would push total to 4) stays in graveyard") {
                game.isOnBattlefield("Hired Claw") shouldBe false
                game.isInGraveyard(1, "Hired Claw") shouldBe true
            }
            withClue("Grizzly Bears was never submitted") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
        }

        test("decision arrives with maxSelections clamped by the greedy fit (2+2+1 ⇒ max 2)") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Scout for Survivors")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInGraveyard(1, "Grizzly Bears")  // MV 2
                .withCardInGraveyard(1, "Glory Seeker")   // MV 2
                .withCardInGraveyard(1, "Hired Claw")     // MV 1
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Scout for Survivors").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("Greedy ceiling on [1, 2, 2] under cap 3 fits 2 cards (1 + 2)") {
                decision.maxSelections shouldBe 2
            }
            withClue("Min stays at 0 — the spell is 'up to'") {
                decision.minSelections shouldBe 0
            }
            withClue("maxTotalManaValue is propagated to the decision so the UI can disable cards live") {
                decision.maxTotalManaValue shouldBe 3
            }
        }

        test("only MV-4 creatures present ⇒ decision allows no selections (greedy ceiling 0)") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Scout for Survivors")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInGraveyard(1, "Hill Giant")     // {3}{R} — MV 4
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Scout for Survivors").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("Hill Giant alone is MV 4 > 3 — no valid selection exists, so max is 0") {
                decision.maxSelections shouldBe 0
            }
            withClue("Player still confirms (min 0) to resolve the spell") {
                decision.minSelections shouldBe 0
            }

            game.selectCards(emptyList()).error shouldBe null
            game.isOnBattlefield("Hill Giant") shouldBe false
            game.isInGraveyard(1, "Hill Giant") shouldBe true
        }

        test("selecting no cards resolves the spell with no creatures returned") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Scout for Survivors")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInGraveyard(1, "Hired Claw")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Scout for Survivors").error shouldBe null
            game.resolveStack()
            game.hasPendingDecision() shouldBe true

            game.selectCards(emptyList()).error shouldBe null

            game.isOnBattlefield("Hired Claw") shouldBe false
            game.isInGraveyard(1, "Hired Claw") shouldBe true
            game.isInGraveyard(1, "Scout for Survivors") shouldBe true
        }
    }
}
