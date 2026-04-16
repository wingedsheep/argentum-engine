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
 * These tests exercise the cast-time choose-N modal flow (rules 601.2b–c / 700.2):
 * modes and per-mode targets are picked *before* the spell hits the stack. The
 * enumerator also applies 700.2a (modes with no legal targets are not offered),
 * so when none of Alice's permanents are Merfolk the first mode is filtered out
 * and only 3 modes are offered.
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

                // Cast-time mode selection (Phase 4). Mode 0 ("Create token copy of target
                // Merfolk you control") is filtered by 700.2a since Alice controls no Merfolk,
                // so the offered indices are [1, 2, 3].
                // First pick (1 of 2): mode 2 ("Target player draws a card") at offered-position 1.
                game.chooseMode(1)
                // Remaining offered indices: [1, 3]. Pick mode 3 ("Tap target creature. Put a
                // stun counter on it") at offered-position 1.
                game.chooseMode(1)

                // Per-mode targets (cast-time), in pick order.
                game.selectTargets(listOf(bob))      // mode 2 (draw) target
                game.selectTargets(listOf(bearsId))  // mode 3 (tap + stun) target

                // Spell is on the stack with pre-chosen modes and per-mode targets — resolve.
                game.resolveStack()

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

                // Offered indices = [1, 2, 3] (mode 0 filtered — no Merfolk).
                // Pick mode 1 (lifelink) at offered-position 0; remaining = [2, 3].
                game.chooseMode(0)
                // Pick mode 2 (draw) at offered-position 0.
                game.chooseMode(0)

                // Per-mode targets at cast time: lifelink → Alice, then draw → Alice.
                game.selectTargets(listOf(alice))
                game.selectTargets(listOf(alice))

                game.resolveStack()

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

                game.chooseMode(0) // lifelink (offered-position 0 of [1, 2, 3])
                game.chooseMode(0) // draw     (offered-position 0 of [2, 3])

                game.selectTargets(listOf(bob))    // lifelink target: Bob
                game.selectTargets(listOf(bob))    // draw target: Bob

                game.resolveStack()

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
