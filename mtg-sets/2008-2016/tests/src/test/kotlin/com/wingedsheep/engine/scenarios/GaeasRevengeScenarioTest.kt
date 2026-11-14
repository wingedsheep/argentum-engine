package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gaea's Revenge — "This creature can't be the target of nongreen spells or abilities from
 * nongreen sources." Unlike hexproof, that binds its own controller as well as opponents.
 */
class GaeasRevengeScenarioTest : ScenarioTestBase() {

    private fun TestGame.offeredTargetsFor(playerNumber: Int, spellName: String) =
        getLegalActions(playerNumber).first { la ->
            val action = la.action
            action is CastSpell && state.getEntity(action.cardId)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == spellName
        }.let { la -> la.targetRequirements?.firstOrNull()?.validTargets ?: la.validTargets.orEmpty() }

    init {
        context("Gaea's Revenge") {
            test("its own controller's nongreen spell can't target it; a green spell can") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gaea's Revenge")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Giant Growth")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .withTurnNumber(3)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val revenge = game.findPermanent("Gaea's Revenge")!!

                game.offeredTargetsFor(1, "Lightning Bolt").contains(revenge) shouldBe false
                game.castSpell(1, "Lightning Bolt", revenge).error shouldNotBe null

                game.offeredTargetsFor(1, "Giant Growth").contains(revenge) shouldBe true
                val growth = game.castSpell(1, "Giant Growth", revenge)
                withClue("Giant Growth should target it: ${growth.error}") { growth.error shouldBe null }
                game.resolveStack()
                game.state.projectedState.getPower(revenge) shouldBe 11
            }

            test("an opponent's nongreen spell can't target it either") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Gaea's Revenge")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .withTurnNumber(3)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val revenge = game.findPermanent("Gaea's Revenge")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val offered = game.offeredTargetsFor(2, "Lightning Bolt")
                offered.contains(revenge) shouldBe false
                offered.contains(bears) shouldBe true
                game.castSpell(2, "Lightning Bolt", revenge).error shouldNotBe null
            }
        }
    }
}
