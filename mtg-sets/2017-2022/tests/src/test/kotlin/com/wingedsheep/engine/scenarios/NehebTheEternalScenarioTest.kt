package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Neheb, the Eternal (HOU #104) — {3}{R}{R} Legendary Creature — Zombie Minotaur Warrior 4/6.
 *
 *   Afflict 3
 *   At the beginning of each of your postcombat main phases, add {R} for each 1 life your
 *   opponents have lost this turn.
 */
class NehebTheEternalScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.redInPool(): Int =
        state.getEntity(player1Id)?.get<ManaPoolComponent>()?.red ?: 0

    init {
        test("unblocked combat damage — adds {R} per life lost, and the mana stays through the main phase") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Neheb, the Eternal", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Neheb, the Eternal" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.getLifeTotal(2) shouldBe 16
            game.resolveStack()

            withClue("The opponent lost 4 life this turn, so Neheb adds {R}{R}{R}{R}") {
                game.state.step shouldBe Step.POSTCOMBAT_MAIN
                game.redInPool() shouldBe 4
            }
        }

        test("life lost to a precombat spell counts, with no attack at all") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Neheb, the Eternal", summoningSickness = false)
                .withCardOnBattlefield(1, "Mountain")
                .withCardInHand(1, "Lightning Bolt")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 17

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.resolveStack()

            withClue("Bolt made the opponent lose 3 life; no attack still gives a postcombat main phase") {
                game.redInPool() shouldBe 3
            }
        }

        test("blocked — afflict makes the defending player lose 3, which Neheb then counts") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Neheb, the Eternal", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Neheb, the Eternal" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Neheb, the Eternal"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.getLifeTotal(2) shouldBe 17
            game.resolveStack()

            game.redInPool() shouldBe 3
        }

        test("no life lost this turn — the trigger adds no mana") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Neheb, the Eternal", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.resolveStack()

            game.redInPool() shouldBe 0
        }
    }
}
