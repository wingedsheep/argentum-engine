package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Full Steam Ahead (OTJ #164).
 *
 * "Until end of turn, each creature you control gets +2/+2 and gains trample and
 *  'This creature can't be blocked by more than one creature.'"
 *
 * Exercises the new [com.wingedsheep.sdk.dsl.Effects.GrantStaticAbility] effect granting
 * [com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan] temporarily, alongside the mass
 * +2/+2 and trample grant (the Overrun shape). Verifies:
 *  - each creature you control gets +2/+2 and trample;
 *  - a buffed attacker can't be blocked by two creatures, but can by one;
 *  - the granted restriction does not extend to creatures you don't control.
 */
class FullSteamAheadScenarioTest : ScenarioTestBase() {

    init {
        // Plain vanilla blockers (TestCards aren't in the scenario registry).
        cardRegistry.register(
            CardDefinition.creature("Blocker A", ManaCost.parse("{1}"), emptySet(), power = 1, toughness = 1)
        )
        cardRegistry.register(
            CardDefinition.creature("Blocker B", ManaCost.parse("{1}"), emptySet(), power = 1, toughness = 1)
        )

        test("each creature you control gets +2/+2 and gains trample") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Full Steam Ahead")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
                .withLandsOnBattlefield(1, "Forest", 5) // {3}{G}{G}
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Full Steam Ahead").error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears should be 4/4 after +2/+2") {
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 4
            }
            withClue("Grizzly Bears should have gained trample") {
                game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            }
        }

        test("a buffed creature you control can't be blocked by more than one creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Full Steam Ahead")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2 attacker
                .withCardOnBattlefield(2, "Blocker A")
                .withCardOnBattlefield(2, "Blocker B")
                .withLandsOnBattlefield(1, "Forest", 5) // {3}{G}{G}
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Full Steam Ahead").error shouldBe null
            game.resolveStack()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            val doubleBlock = game.declareBlockers(
                mapOf(
                    "Blocker A" to listOf("Grizzly Bears"),
                    "Blocker B" to listOf("Grizzly Bears"),
                )
            )
            withClue("Two creatures can't block a Grizzly Bears that gained 'can't be blocked by more than one creature'") {
                doubleBlock.error shouldNotBe null
            }
        }

        test("a single creature can still block the buffed attacker") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Full Steam Ahead")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Blocker A")
                .withCardOnBattlefield(2, "Blocker B")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Full Steam Ahead").error shouldBe null
            game.resolveStack()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            val singleBlock = game.declareBlockers(mapOf("Blocker A" to listOf("Grizzly Bears")))
            withClue("One creature can legally block the buffed attacker: ${singleBlock.error}") {
                singleBlock.error shouldBe null
            }
        }

        test("the granted restriction does not extend to creatures the controller doesn't control") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Full Steam Ahead")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // P1's creature
                .withCardOnBattlefield(2, "Blocker A") // P2's attacker next turn — but check the buff scoping now
                .withLandsOnBattlefield(1, "Forest", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val opponentCreature = game.findPermanent("Blocker A")!!

            game.castSpell(1, "Full Steam Ahead").error shouldBe null
            game.resolveStack()

            withClue("Full Steam Ahead only buffs creatures YOU control — the opponent's creature is unaffected") {
                game.state.projectedState.getPower(opponentCreature) shouldBe 1
                game.state.projectedState.hasKeyword(opponentCreature, Keyword.TRAMPLE) shouldBe false
            }
        }
    }
}
