package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Forgotten Ancient.
 *
 * Card reference:
 * - Forgotten Ancient ({3}{G}): Creature — Elemental 0/3
 *   Whenever a player casts a spell, you may put a +1/+1 counter on Forgotten Ancient.
 *   At the beginning of your upkeep, you may move any number of +1/+1 counters from
 *   Forgotten Ancient onto other creatures.
 */
class ForgottenAncientScenarioTest : ScenarioTestBase() {

    /**
     * Advance from UNTAP to UPKEEP and wait for the MayEffect yes/no decision.
     * Unlike passUntilPhase, this does NOT auto-resolve decisions.
     */
    private fun ScenarioTestBase.TestGame.advanceToUpkeepTrigger() {
        var iterations = 0
        while (iterations < 50) {
            if (hasPendingDecision()) break
            val p = state.priorityPlayerId ?: break
            execute(PassPriority(p))
            iterations++
        }
    }

    init {
        context("Forgotten Ancient - spell cast trigger") {

            test("gains a +1/+1 counter when controller casts a spell and says yes") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Forgotten Ancient")
                    .withCardInHand(1, "Shock")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ancientId = game.findPermanent("Forgotten Ancient")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Cast Shock targeting Grizzly Bears
                game.castSpell(1, "Shock", bearsId)
                // Spell cast triggers Forgotten Ancient's ability which goes on the stack
                // Resolve the triggered ability (it resolves before the spell)
                game.resolveStack()

                // MayEffect creates a yes/no decision during resolution
                val decision = game.state.pendingDecision
                withClue("Should have a yes/no decision for the may effect") {
                    decision.shouldBeInstanceOf<YesNoDecision>()
                }

                game.answerYesNo(true)

                // Check that Forgotten Ancient has a +1/+1 counter
                val counters = game.state.getEntity(ancientId)?.get<CountersComponent>()
                withClue("Forgotten Ancient should have 1 +1/+1 counter") {
                    counters shouldNotBe null
                    counters!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
            }

            test("does not gain a counter when controller says no") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Forgotten Ancient")
                    .withCardInHand(1, "Shock")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ancientId = game.findPermanent("Forgotten Ancient")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Shock", bearsId)
                game.resolveStack()

                // Decline the may effect
                game.answerYesNo(false)

                // Should have no counters
                val counters = game.state.getEntity(ancientId)?.get<CountersComponent>()
                val count = counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                withClue("Forgotten Ancient should have 0 +1/+1 counters") {
                    count shouldBe 0
                }
            }

            test("triggers when opponent casts a spell") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Forgotten Ancient")
                    .withCardInHand(2, "Shock")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ancientId = game.findPermanent("Forgotten Ancient")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Opponent casts Shock on Player 1's creature
                game.castSpell(2, "Shock", bearsId)
                // Resolve the triggered ability
                game.resolveStack()

                // Player 1 gets the may effect decision
                val decision = game.state.pendingDecision
                withClue("Should have a yes/no decision for the may effect") {
                    decision.shouldBeInstanceOf<YesNoDecision>()
                }

                game.answerYesNo(true)

                val counters = game.state.getEntity(ancientId)?.get<CountersComponent>()
                withClue("Forgotten Ancient should have 1 +1/+1 counter") {
                    counters shouldNotBe null
                    counters!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
            }
        }

        context("Forgotten Ancient - distribute counters on upkeep") {

            test("can move counters from self to other creatures on upkeep") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Forgotten Ancient")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val ancientId = game.findPermanent("Forgotten Ancient")!!
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val seekerId = game.findPermanent("Glory Seeker")!!

                // Manually put 3 +1/+1 counters on Forgotten Ancient
                game.state = game.state.updateEntity(ancientId) { container ->
                    container.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 3))
                }

                // Advance from UNTAP to UPKEEP, stopping at the MayEffect decision
                game.advanceToUpkeepTrigger()

                // MayEffect should present yes/no
                val decision = game.state.pendingDecision
                withClue("Should have a yes/no decision for the may effect") {
                    decision.shouldBeInstanceOf<YesNoDecision>()
                }

                game.answerYesNo(true)

                // Should now have a distribute decision
                val distributeDecision = game.state.pendingDecision
                withClue("Should have a distribute decision") {
                    distributeDecision.shouldBeInstanceOf<DistributeDecision>()
                }
                val dd = distributeDecision as DistributeDecision
                withClue("Total should be 3 counters") {
                    dd.totalAmount shouldBe 3
                }

                // Move 2 counters to bears, 1 to seeker
                game.submitDistribution(mapOf(bearsId to 2, seekerId to 1))

                // Check counter distribution
                val ancientCounters = game.state.getEntity(ancientId)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                val bearsCounters = game.state.getEntity(bearsId)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                val seekerCounters = game.state.getEntity(seekerId)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

                withClue("Forgotten Ancient should have 0 counters left") {
                    ancientCounters shouldBe 0
                }
                withClue("Grizzly Bears should have 2 +1/+1 counters") {
                    bearsCounters shouldBe 2
                }
                withClue("Glory Seeker should have 1 +1/+1 counter") {
                    seekerCounters shouldBe 1
                }
            }

            test("can decline to move counters on upkeep") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Forgotten Ancient")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val ancientId = game.findPermanent("Forgotten Ancient")!!

                // Put 2 +1/+1 counters
                game.state = game.state.updateEntity(ancientId) { container ->
                    container.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 2))
                }

                // Advance from UNTAP to UPKEEP, stopping at the MayEffect decision
                game.advanceToUpkeepTrigger()

                // Decline the may effect
                game.answerYesNo(false)

                // Counters should remain on Forgotten Ancient
                val counters = game.state.getEntity(ancientId)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                withClue("Forgotten Ancient should still have 2 counters") {
                    counters shouldBe 2
                }
            }

            test("skips distribution if no counters on self") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Forgotten Ancient")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // No counters on Forgotten Ancient

                // Advance from UNTAP to UPKEEP, stopping at the MayEffect decision
                game.advanceToUpkeepTrigger()

                // Say yes to the may effect
                game.answerYesNo(true)

                // Effect should resolve silently since there are 0 counters
                // No distribute decision should appear
                val decision = game.state.pendingDecision
                withClue("No distribution decision should appear when there are no counters") {
                    decision shouldBe null
                }
            }
        }
    }
}
