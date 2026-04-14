package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Sygg's Command.
 *
 * Card reference ({1}{W}{U} Kindred Sorcery — Merfolk):
 * Choose two —
 *   0. Create a token that's a copy of target Merfolk you control.
 *   1. Creatures target player controls gain lifelink until end of turn.
 *   2. Target player draws a card.
 *   3. Tap target creature. Put a stun counter on it.
 *
 * These tests primarily exercise the engine's Choose-N modal spell flow:
 * the player picks two modes iteratively, then each chosen mode resolves
 * in order (pausing for its own target selection when required).
 */
class SyggsCommandScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private fun TestGame.chooseMode(optionIndex: Int) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(OptionChosenResponse(decision.id, optionIndex))
    }

    init {
        context("Sygg's Command — choose-two modal") {

            test("draw-a-card + tap-and-stun: both modes resolve in pick order") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Sygg's Command")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bob = game.player2Id
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val bobHandBefore = game.state.getHand(bob).size

                game.castSpell(1, "Sygg's Command")
                game.resolveStack()

                // First pick (1 of 2): mode 2 = "Target player draws a card" at original index 2.
                game.chooseMode(2)
                // Second pick (2 of 2): remaining options are modes [0, 1, 3]; pick mode 3
                // ("Tap target creature. Put a stun counter on it") which is now at position 2.
                game.chooseMode(2)

                // Mode 2 (draw) resolves first — pause for target player selection.
                game.selectTargets(listOf(bob))

                // Mode 3 (tap + stun) resolves next — pause for target creature.
                game.selectTargets(listOf(bearsId))

                withClue("Bob should have drawn a card") {
                    game.state.getHand(bob).size shouldBe bobHandBefore + 1
                }
                withClue("Grizzly Bears should be tapped") {
                    game.state.getEntity(bearsId)?.get<TappedComponent>().shouldNotBeNull()
                }
                withClue("Grizzly Bears should have a stun counter") {
                    val counters = game.state.getEntity(bearsId)?.get<CountersComponent>()
                    counters?.getCount(CounterType.STUN) shouldBe 1
                }
                withClue("Sygg's Command should be in the graveyard after resolving") {
                    game.isInGraveyard(1, "Sygg's Command") shouldBe true
                }
            }

            test("lifelink-to-target-player applies to the targeted player's creatures (self-target)") {
                // Regression test: targeting yourself with the lifelink mode must grant lifelink
                // to *your* creatures, not your opponent's. This exercises the PredicateContext
                // fix where ControlledByTargetPlayer resolves against the actually chosen target
                // instead of defaulting to the spell controller's opponent.
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Sygg's Command")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")  // Alice's creature
                    .withCardOnBattlefield(2, "Hill Giant")     // Bob's creature
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val alice = game.player1Id
                val aliceBearsId = game.findPermanent("Grizzly Bears")!!
                val bobGiantId = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Sygg's Command")
                game.resolveStack()

                // Pick mode 1 (lifelink to target player's creatures) then mode 2 (draw) as filler.
                game.chooseMode(1)
                // Remaining: [0, 2, 3]. Pick mode 2 "Target player draws a card" at position 1.
                game.chooseMode(1)

                // First resolves: lifelink mode — target Alice herself.
                game.selectTargets(listOf(alice))
                // Then draw mode — target Alice.
                game.selectTargets(listOf(alice))

                val projected = stateProjector.project(game.state)
                withClue("Alice's Grizzly Bears should gain lifelink") {
                    projected.hasKeyword(aliceBearsId, Keyword.LIFELINK) shouldBe true
                }
                withClue("Bob's Hill Giant must NOT gain lifelink (opponent was not the target)") {
                    projected.hasKeyword(bobGiantId, Keyword.LIFELINK) shouldBe false
                }
            }

            test("lifelink-to-target-player applies to opponent's creatures when opponent is targeted") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Sygg's Command")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bob = game.player2Id
                val aliceBearsId = game.findPermanent("Grizzly Bears")!!
                val bobGiantId = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Sygg's Command")
                game.resolveStack()

                game.chooseMode(1) // lifelink
                game.chooseMode(1) // draw (mode 2 at position 1 of [0,2,3])

                game.selectTargets(listOf(bob))    // lifelink target: Bob
                game.selectTargets(listOf(bob))    // draw target: Bob

                val projected = stateProjector.project(game.state)
                withClue("Bob's Hill Giant should gain lifelink") {
                    projected.hasKeyword(bobGiantId, Keyword.LIFELINK) shouldBe true
                }
                withClue("Alice's Grizzly Bears must not gain lifelink") {
                    projected.hasKeyword(aliceBearsId, Keyword.LIFELINK) shouldBe false
                }
            }
        }
    }
}
