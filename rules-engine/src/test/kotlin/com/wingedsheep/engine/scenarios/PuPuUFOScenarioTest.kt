package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fin.cards.PuPuUFO
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** Scenario tests for PuPu UFO. */
class PuPuUFOScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("PuPu UFO") {
            test("{3} sets its base power to the number of Towns you control") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "PuPu UFO", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Capital City", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ufo = game.findPermanent("PuPu UFO")!!
                val pump = PuPuUFO.activatedAbilities[1].id

                val before = stateProjector.project(game.state)
                withClue("PuPu UFO starts as a 0/4") {
                    before.getPower(ufo) shouldBe 0
                    before.getToughness(ufo) shouldBe 4
                }

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = ufo, abilityId = pump)
                )
                withClue("Activating the pump should succeed: ${result.error}") { result.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val after = stateProjector.project(game.state)
                withClue("Base power becomes the number of Towns you control (3); toughness unchanged") {
                    after.getPower(ufo) shouldBe 3
                    after.getToughness(ufo) shouldBe 4
                }
            }

            test("{T} puts a land card from hand onto the battlefield") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "PuPu UFO", summoningSickness = false)
                    .withCardInHand(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ufo = game.findPermanent("PuPu UFO")!!
                val putLand = PuPuUFO.activatedAbilities[0].id

                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = ufo, abilityId = putLand)
                )
                withClue("Activating the land-drop should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(decision.options.first()))
                game.resolveStack()

                withClue("The Forest should now be on the battlefield") {
                    game.findPermanent("Forest") shouldNotBe null
                }
                withClue("The Forest left Alice's hand") {
                    game.state.getHand(game.player1Id).count { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Forest"
                    } shouldBe 0
                }
            }
        }
    }
}
