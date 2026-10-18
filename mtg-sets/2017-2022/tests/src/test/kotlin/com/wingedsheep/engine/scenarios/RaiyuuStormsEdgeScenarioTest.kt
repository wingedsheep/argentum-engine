package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.AdditionalPhasesComponent
import com.wingedsheep.engine.state.components.player.ExtraPhaseKind
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Raiyuu, Storm's Edge (NEO #232) — "Whenever a Samurai or Warrior you control attacks alone, untap
 * it. If it's the first combat phase of the turn, there is an additional combat phase after this
 * phase."
 *
 * The untap applies in every combat; the extra combat only in the first, so the loop stops at two.
 */
class RaiyuuStormsEdgeScenarioTest : ScenarioTestBase() {

    init {
        context("Raiyuu, Storm's Edge") {

            test("a Warrior attacking alone untaps and adds exactly one extra combat") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Raiyuu, Storm's Edge")
                    .withCardOnBattlefield(1, "Centaur Courser") // 3/3 Centaur Warrior
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                fun queued() = game.state.getEntity(game.state.activePlayerId!!)
                    ?.get<AdditionalPhasesComponent>()?.phases.orEmpty()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the lone attacker untaps") {
                    game.state.getEntity(courser)!!.has<TappedComponent>() shouldBe false
                }
                withClue("one additional combat phase is queued") {
                    queued().map { it.kind } shouldBe listOf(ExtraPhaseKind.COMBAT)
                }

                // Into the additional combat: the untap fires again, but no third combat.
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.state.step shouldBe Step.DECLARE_ATTACKERS
                game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null
                game.resolveStack()

                withClue("second combat: untapped again, nothing more queued") {
                    game.state.getEntity(courser)!!.has<TappedComponent>() shouldBe false
                    queued().size shouldBe 0
                }
            }

            test("attacking with two creatures doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Raiyuu, Storm's Edge")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Raiyuu, Storm's Edge" to 2, "Centaur Courser" to 2)).error shouldBe null
                game.resolveStack()

                game.state.getEntity(game.findPermanent("Centaur Courser")!!)!!.has<TappedComponent>() shouldBe true
                game.state.getEntity(game.findPermanent("Raiyuu, Storm's Edge")!!)!!.has<TappedComponent>() shouldBe true
                game.state.getEntity(game.state.activePlayerId!!)
                    ?.get<AdditionalPhasesComponent>()?.phases.orEmpty().size shouldBe 0
            }
        }
    }
}
