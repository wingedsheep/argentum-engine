package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Combat Celebrant (AKH #125).
 *
 *   If this creature hasn't been exerted this turn, you may exert it as it attacks. When you do,
 *   untap all other creatures you control and after this phase, there is an additional combat phase.
 */
class CombatCelebrantScenarioTest : ScenarioTestBase() {

    init {
        context("Combat Celebrant") {

            fun game() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Combat Celebrant")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("exerting untaps the other creatures and adds one combat phase") {
                val game = game()
                val celebrant = game.findPermanent("Combat Celebrant")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Combat Celebrant" to 2, "Grizzly Bears" to 2)).error shouldBe null
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>().options shouldBe listOf(celebrant)
                game.selectCards(listOf(celebrant))
                game.resolveStack()

                withClue("the other attacker untaps; the exerted Celebrant stays tapped") {
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
                    game.state.getEntity(celebrant)!!.has<TappedComponent>() shouldBe true
                    game.state.getEntity(celebrant)!!.has<ExertedComponent>() shouldBe true
                }
                val queued = game.state.getEntity(game.state.activePlayerId!!)
                    ?.get<AdditionalPhasesComponent>()?.phases.orEmpty()
                queued.size shouldBe 1
                queued.single().kind shouldBe ExtraPhaseKind.COMBAT
            }

            test("declining the exert does nothing") {
                val game = game()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Combat Celebrant" to 2, "Grizzly Bears" to 2)).error shouldBe null
                game.selectCards(emptyList())

                game.state.stack.isEmpty() shouldBe true
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                game.state.getEntity(game.state.activePlayerId!!)
                    ?.get<AdditionalPhasesComponent>()?.phases.orEmpty().size shouldBe 0
            }

            test("it can't be exerted again in the additional combat the same turn") {
                val game = game()
                val celebrant = game.findPermanent("Combat Celebrant")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Combat Celebrant" to 2, "Grizzly Bears" to 2)).error shouldBe null
                game.selectCards(listOf(celebrant))
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.state.step shouldBe Step.DECLARE_ATTACKERS
                // Something else untapped it (say, a second Celebrant) — it can attack, but not exert.
                game.state = game.state.updateEntity(celebrant) { it.without<TappedComponent>() }

                game.declareAttackers(mapOf("Combat Celebrant" to 2, "Grizzly Bears" to 2)).error shouldBe null
                withClue("no exert offer, so no further combat phase") {
                    game.getPendingDecision() shouldBe null
                    game.state.stack.isEmpty() shouldBe true
                    game.state.getEntity(game.state.activePlayerId!!)
                        ?.get<AdditionalPhasesComponent>()?.phases.orEmpty().size shouldBe 0
                }
            }
        }
    }
}
