package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Verdant Outrider. */
class VerdantOutriderScenarioTest : ScenarioTestBase() {

    private val outriderAbilityId by lazy {
        cardRegistry.requireCard("Verdant Outrider").activatedAbilities[0].id
    }

    init {
        context("Verdant Outrider — activated blocking restriction") {
            test("after activating, a power-2 creature can't block it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Verdant Outrider", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val outrider = game.findPermanent("Verdant Outrider")!!

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = outrider, abilityId = outriderAbilityId)
                ).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Verdant Outrider" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("the 2/2 Bears has power 2, so the restriction forbids the block") {
                    game.declareBlockers(
                        mapOf("Grizzly Bears" to listOf("Verdant Outrider"))
                    ).error shouldNotBe null
                }
            }

            test("without activating, the same creature blocks fine") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Verdant Outrider", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Verdant Outrider" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                game.declareBlockers(
                    mapOf("Grizzly Bears" to listOf("Verdant Outrider"))
                ).error shouldBe null
            }
        }
    }
}
