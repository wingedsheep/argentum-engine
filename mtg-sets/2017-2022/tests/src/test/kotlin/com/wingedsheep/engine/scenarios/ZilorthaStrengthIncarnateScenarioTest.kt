package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Zilortha, Strength Incarnate (IKO #275).
 *
 * "Trample / Lethal damage dealt to creatures you control is determined by their power rather than
 *  their toughness."
 *
 * Wall of Stone (0/8) is the probe: by toughness it shrugs off 2 damage, by power any damage is
 * lethal. Covers the lethal-damage state-based action, "you control" scoping, damage marked before
 * Zilortha enters (ruling), a 0-power creature with no damage surviving (ruling), the rule surviving
 * an affected creature losing its abilities but ending when Zilortha loses its own, trample's lethal
 * assignment (ruling) and excess damage (ruling — Hell to Pay).
 */
class ZilorthaStrengthIncarnateScenarioTest : ScenarioTestBase() {

    private val zilortha = "Zilortha, Strength Incarnate"
    private val wall = "Wall of Stone"

    private fun TestGame.resolveWithTargets(target: String) {
        var guard = 0
        while (guard++ < 10) {
            if (getPendingDecision() != null) {
                selectTargets(listOf(findPermanent(target)!!)).error shouldBe null
            } else if (state.stack.isNotEmpty()) {
                resolveStack()
            } else return
        }
    }

    init {
        test("a creature you control dies to damage equal to its power; an opponent's doesn't") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, zilortha)
                .withCardOnBattlefield(1, wall)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInHand(1, "Shock")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("a 0-power creature with no damage marked isn't destroyed") {
                game.isOnBattlefield(wall) shouldBe true
            }
            game.castSpell(1, "Shock", game.findPermanent(wall)).error shouldBe null
            game.resolveStack()
            withClue("2 damage is lethal to a 0/8 judged by power") {
                game.isOnBattlefield(wall) shouldBe false
            }
            game.castSpell(1, "Shock", game.findPermanent("Hill Giant")).error shouldBe null
            game.resolveStack()
            withClue("the opponent's 3/3 is judged by toughness") {
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }

        test("damage marked before Zilortha enters becomes lethal") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, wall)
                .withCardInHand(1, "Shock")
                .withCardInHand(1, zilortha)
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Shock", game.findPermanent(wall)).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield(wall) shouldBe true
            game.castSpell(1, zilortha).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield(zilortha) shouldBe true
            game.isOnBattlefield(wall) shouldBe false
        }

        test("an affected creature losing all abilities is still judged by power") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, zilortha)
                .withCardOnBattlefield(1, wall)
                .withCardInHand(2, "Merfolk Trickster")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Island", 2)
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Merfolk Trickster").error shouldBe null
            game.resolveWithTargets(wall)
            game.state.projectedState.hasLostAllAbilities(game.findPermanent(wall)!!) shouldBe true
            game.castSpell(2, "Shock", game.findPermanent(wall)).error shouldBe null
            game.resolveStack()
            withClue("the rule comes from Zilortha, not from an ability the Wall has") {
                game.isOnBattlefield(wall) shouldBe false
            }
        }

        test("Zilortha losing its abilities ends the rule") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, zilortha)
                .withCardOnBattlefield(1, wall)
                .withCardInHand(2, "Merfolk Trickster")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Island", 2)
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Merfolk Trickster").error shouldBe null
            game.resolveWithTargets(zilortha)
            game.castSpell(2, "Shock", game.findPermanent(wall)).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield(wall) shouldBe true
        }

        test("trample only has to assign lethal damage by power to a blocker under the rule") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Colossal Dreadmaw")
                .withCardOnBattlefield(2, zilortha)
                .withCardOnBattlefield(2, wall)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Colossal Dreadmaw" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf(wall to listOf("Colossal Dreadmaw"))).error shouldBe null

            var passes = 0
            while (!game.hasPendingDecision() && passes++ < 20) game.passPriority()
            val decision = game.getPendingDecision().shouldBeInstanceOf<CombatResolutionDecision>()
            val wallEdge = decision.edges.single { it.targetId == game.findPermanent(wall) }
            withClue("lethal for a 0-power Wall is 1 under its controller's Zilortha") {
                wallEdge.lethal shouldBe 1
            }
            game.submitCombatDamage(
                mapOf(
                    (wallEdge.sourceId to wallEdge.targetId) to 1,
                    (wallEdge.sourceId to game.player2Id) to 5
                )
            ).error shouldBe null
            game.getLifeTotal(2) shouldBe 15
            game.isOnBattlefield(wall) shouldBe false
        }

        test("excess damage is measured past power") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, zilortha)
                .withCardOnBattlefield(1, wall)
                .withCardInHand(1, "Hell to Pay")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Hell to Pay", 3, game.findPermanent(wall)).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield(wall) shouldBe false
            withClue("3 damage to a 0-power creature: 1 is lethal, 2 are excess") {
                game.findPermanents("Treasure").size shouldBe 2
            }
        }
    }
}
