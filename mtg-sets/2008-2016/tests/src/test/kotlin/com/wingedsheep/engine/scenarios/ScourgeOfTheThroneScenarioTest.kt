package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderTriggersDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.TriggerOrderResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scourge of the Throne (CNS #35).
 *
 *   Dethrone (Whenever this creature attacks the player with the most life or tied for most life,
 *   put a +1/+1 counter on it.)
 *   Whenever this creature attacks for the first time each turn, if it's attacking the player with
 *   the most life or tied for most life, untap all attacking creatures. After this phase, there is
 *   an additional combat phase.
 *
 * Pins the first dethrone card and the extra-combat loop: the "first time each turn" gate keeps
 * the second attack from queuing another combat, while dethrone fires on every attack.
 */
class ScourgeOfTheThroneScenarioTest : ScenarioTestBase() {

    private fun counters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Dethrone and the extra-combat trigger fire together; their order doesn't matter here. */
    private fun TestGame.resolveAll() {
        repeat(10) {
            when (val decision = state.pendingDecision) {
                is OrderTriggersDecision ->
                    submitDecision(TriggerOrderResponse(decision.id, decision.triggers.indices.toList()))
                is OrderObjectsDecision -> submitDecision(OrderedResponse(decision.id, decision.objects))
                else -> {}
            }
            resolveStack()
        }
    }

    private fun queuedPhases(game: TestGame) =
        game.state.getEntity(game.state.activePlayerId!!)?.get<AdditionalPhasesComponent>()?.phases.orEmpty()

    init {
        context("Scourge of the Throne") {

            test("attacking the player with the most life: counter, untap attackers, one extra combat") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Scourge of the Throne")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLifeTotal(1, 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scourge = game.findPermanent("Scourge of the Throne")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Scourge of the Throne" to 2, "Grizzly Bears" to 2)).error shouldBe null
                game.resolveAll()

                withClue("dethrone puts a +1/+1 counter on it") { counters(game, scourge) shouldBe 1 }
                withClue("all attacking creatures untap") {
                    game.state.getEntity(scourge)!!.has<TappedComponent>() shouldBe false
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
                }
                withClue("exactly one additional combat phase is queued") {
                    queuedPhases(game).size shouldBe 1
                    queuedPhases(game).single().kind shouldBe ExtraPhaseKind.COMBAT
                }

                // The additional combat: dethrone fires again, the first-time trigger doesn't.
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.state.step shouldBe Step.DECLARE_ATTACKERS
                game.declareAttackers(mapOf("Scourge of the Throne" to 2)).error shouldBe null
                game.resolveAll()

                withClue("Bob still has the most life, so dethrone puts a second counter on it") {
                    counters(game, scourge) shouldBe 2
                }
                withClue("the second attack isn't the first this turn — it stays tapped, no new phase") {
                    game.state.getEntity(scourge)!!.has<TappedComponent>() shouldBe true
                    queuedPhases(game).size shouldBe 0
                }
            }

            test("attacking a player who doesn't have the most life does nothing") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Scourge of the Throne")
                    .withLifeTotal(2, 10)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scourge = game.findPermanent("Scourge of the Throne")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Scourge of the Throne" to 2)).error shouldBe null
                game.resolveAll()

                counters(game, scourge) shouldBe 0
                game.state.getEntity(scourge)!!.has<TappedComponent>() shouldBe true
                queuedPhases(game).size shouldBe 0
            }
        }
    }
}
