package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.akh.cards.DesiccatedNaga
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Desiccated Naga (AKH #276) — {2}{B} Creature — Zombie Snake 3/2.
 *
 *   {3}{B}: Target opponent loses 2 life and you gain 2 life. Activate only if you control a
 *   Liliana planeswalker.
 */
class DesiccatedNagaScenarioTest : ScenarioTestBase() {

    private val abilityId = DesiccatedNaga.activatedAbilities.single().id

    init {
        context("Desiccated Naga") {

            test("drains 2 while you control a Liliana planeswalker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Desiccated Naga")
                    .withCardOnBattlefield(1, "Liliana Vess")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val naga = game.findPermanent("Desiccated Naga")!!

                val result = game.execute(
                    ActivateAbility(game.player1Id, naga, abilityId, listOf(ChosenTarget.Player(game.player2Id)))
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 22
            }

            test("can't be activated without a Liliana planeswalker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Desiccated Naga")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val naga = game.findPermanent("Desiccated Naga")!!

                game.execute(
                    ActivateAbility(game.player1Id, naga, abilityId, listOf(ChosenTarget.Player(game.player2Id)))
                ).error shouldNotBe null
            }

            test("an opponent's Liliana doesn't count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Desiccated Naga")
                    .withCardOnBattlefield(2, "Liliana Vess")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val naga = game.findPermanent("Desiccated Naga")!!

                game.execute(
                    ActivateAbility(game.player1Id, naga, abilityId, listOf(ChosenTarget.Player(game.player2Id)))
                ).error shouldNotBe null
            }
        }
    }
}
